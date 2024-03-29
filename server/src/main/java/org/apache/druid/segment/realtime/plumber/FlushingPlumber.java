/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.druid.segment.realtime.plumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.druid.client.cache.Cache;
import org.apache.druid.client.cache.CacheConfig;
import org.apache.druid.client.cache.CachePopulatorStats;
import org.apache.druid.common.guava.ThreadRenamingCallable;
import org.apache.druid.java.util.common.DateTimes;
import org.apache.druid.java.util.common.FileUtils;
import org.apache.druid.java.util.common.StringUtils;
import org.apache.druid.java.util.common.concurrent.Execs;
import org.apache.druid.java.util.common.concurrent.ScheduledExecutors;
import org.apache.druid.java.util.common.granularity.Granularity;
import org.apache.druid.java.util.emitter.EmittingLogger;
import org.apache.druid.java.util.emitter.service.ServiceEmitter;
import org.apache.druid.query.QueryProcessingPool;
import org.apache.druid.query.QueryRunnerFactoryConglomerate;
import org.apache.druid.segment.IndexIO;
import org.apache.druid.segment.IndexMerger;
import org.apache.druid.segment.indexing.DataSchema;
import org.apache.druid.segment.indexing.RealtimeTuningConfig;
import org.apache.druid.segment.join.JoinableFactory;
import org.apache.druid.segment.realtime.FireDepartmentMetrics;
import org.apache.druid.server.coordination.DataSegmentAnnouncer;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;

/**
 */
public class FlushingPlumber extends RealtimePlumber
{
  private static final EmittingLogger log = new EmittingLogger(FlushingPlumber.class);

  private final DataSchema schema;
  private final RealtimeTuningConfig config;
  private final Duration flushDuration;

  private volatile ScheduledExecutorService flushScheduledExec = null;
  private volatile boolean stopped = false;

  public FlushingPlumber(
      Duration flushDuration,
      DataSchema schema,
      RealtimeTuningConfig config,
      FireDepartmentMetrics metrics,
      ServiceEmitter emitter,
      QueryRunnerFactoryConglomerate conglomerate,
      DataSegmentAnnouncer segmentAnnouncer,
      QueryProcessingPool queryProcessingPool,
      JoinableFactory joinableFactory,
      IndexMerger indexMerger,
      IndexIO indexIO,
      Cache cache,
      CacheConfig cacheConfig,
      CachePopulatorStats cachePopulatorStats,
      ObjectMapper objectMapper

  )
  {
    super(
        schema,
        config,
        metrics,
        emitter,
        conglomerate,
        segmentAnnouncer,
        queryProcessingPool,
        null,
        null,
        null,
        indexMerger,
        indexIO,
        cache,
        cacheConfig,
        cachePopulatorStats,
        objectMapper
    );

    this.flushDuration = flushDuration;
    this.schema = schema;
    this.config = config;
  }

  @Override
  public Object startJob()
  {
    log.info("Starting job for %s", getSchema().getDataSource());

    try {
      FileUtils.mkdirp(computeBaseDir(getSchema()));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

    initializeExecutors();

    if (flushScheduledExec == null) {
      flushScheduledExec = Execs.scheduledSingleThreaded("flushing_scheduled_%d");
    }

    Object retVal = bootstrapSinksFromDisk();
    startFlushThread();
    return retVal;
  }

  protected void flushAfterDuration(final long truncatedTime, final Sink sink)
  {
    log.info(
        "Abandoning segment %s at %s",
        sink.getSegment().getId(),
        DateTimes.nowUtc().plusMillis((int) flushDuration.getMillis())
    );

    ScheduledExecutors.scheduleWithFixedDelay(
        flushScheduledExec,
        flushDuration,
        new Callable<ScheduledExecutors.Signal>()
        {
          @Override
          public ScheduledExecutors.Signal call()
          {
            log.info("Abandoning segment %s", sink.getSegment().getId());
            abandonSegment(truncatedTime, sink);
            return ScheduledExecutors.Signal.STOP;
          }
        }
    );
  }

  private void startFlushThread()
  {
    final Granularity segmentGranularity = schema.getGranularitySpec().getSegmentGranularity();
    final DateTime truncatedNow = segmentGranularity.bucketStart(DateTimes.nowUtc());
    final long windowMillis = config.getWindowPeriod().toStandardDuration().getMillis();

    log.info(
        "Expect to run at [%s]",
        DateTimes.nowUtc().plus(
            new Duration(
                System.currentTimeMillis(),
                schema.getGranularitySpec().getSegmentGranularity().increment(truncatedNow).getMillis() + windowMillis
            )
        )
    );

    String threadName = StringUtils.format(
        "%s-flusher-%d",
        getSchema().getDataSource(),
        getConfig().getShardSpec().getPartitionNum()
    );
    ThreadRenamingCallable<ScheduledExecutors.Signal> threadRenamingCallable =
        new ThreadRenamingCallable<ScheduledExecutors.Signal>(threadName)
        {
          @Override
          public ScheduledExecutors.Signal doCall()
          {
            if (stopped) {
              log.info("Stopping flusher thread");
              return ScheduledExecutors.Signal.STOP;
            }

            long minTimestamp = segmentGranularity.bucketStart(
                getRejectionPolicy().getCurrMaxTime().minus(windowMillis)
            ).getMillis();

            List<Map.Entry<Long, Sink>> sinksToPush = new ArrayList<>();
            for (Map.Entry<Long, Sink> entry : getSinks().entrySet()) {
              final Long intervalStart = entry.getKey();
              if (intervalStart < minTimestamp) {
                log.info("Adding entry[%s] to flush.", entry);
                sinksToPush.add(entry);
              }
            }

            for (final Map.Entry<Long, Sink> entry : sinksToPush) {
              flushAfterDuration(entry.getKey(), entry.getValue());
            }

            if (stopped) {
              log.info("Stopping flusher thread");
              return ScheduledExecutors.Signal.STOP;
            } else {
              return ScheduledExecutors.Signal.REPEAT;
            }
          }
        };
    Duration initialDelay = new Duration(
        System.currentTimeMillis(),
        schema.getGranularitySpec().getSegmentGranularity().increment(truncatedNow).getMillis() + windowMillis
    );
    Duration rate = new Duration(truncatedNow, segmentGranularity.increment(truncatedNow));
    ScheduledExecutors.scheduleAtFixedRate(flushScheduledExec, initialDelay, rate, threadRenamingCallable);
  }

  @Override
  public void finishJob()
  {
    log.info("Stopping job");

    for (final Map.Entry<Long, Sink> entry : getSinks().entrySet()) {
      abandonSegment(entry.getKey(), entry.getValue());
    }
    shutdownExecutors();

    if (flushScheduledExec != null) {
      flushScheduledExec.shutdown();
    }

    stopped = true;
  }
}
