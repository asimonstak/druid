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

package org.apache.druid.emitter.ambari.metrics;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.io.IOUtils;
import org.apache.druid.annotations.UsedByJUnitParamsRunner;
import org.apache.druid.jackson.DefaultObjectMapper;
import org.apache.druid.java.util.emitter.service.ServiceMetricEvent;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

@RunWith(JUnitParamsRunner.class)
public class WhiteListBasedDruidToTimelineEventConverterTest
{
  private final String prefix = "druid";
  private final WhiteListBasedDruidToTimelineEventConverter defaultWhiteListBasedDruidToTimelineEventConverter =
      new WhiteListBasedDruidToTimelineEventConverter(prefix, "druid", null, new DefaultObjectMapper());
  private final String hostname = "testHost:8080";
  private final String serviceName = "historical";
  private final String defaultNamespace = prefix + "." + serviceName;

  @Test
  @Parameters(
      {
          "query/time, true",
          "query/node/ttfb, true",
          "query/segmentAndCache/time, true",
          "query/time/balaba, true",
          "query/tim, false",
          "segment/added/bytes, true",
          "segment/count, true",
          "segment/size, true",
          "segment/cost/raw, false",
          "coordinator/TIER_1 /cost/raw, false",
          "segment/Kost/raw, false",
          ", false",
          "word, false",
          "coordinator, false",
          "server/, false",
          "ingest/persists/time, true",
          "jvm/mem/init, true",
          "jvm/gc/count, true"
      }
  )
  public void testDefaultIsInWhiteList(String key, boolean expectedValue)
  {
    ServiceMetricEvent event = ServiceMetricEvent
        .builder()
        .setFeed("metrics")
        .setMetric(key, 10)
        .build(serviceName, hostname);

    boolean isIn = defaultWhiteListBasedDruidToTimelineEventConverter.druidEventToTimelineMetric(event) != null;
    Assert.assertEquals(expectedValue, isIn);
  }

  @Test
  @Parameters
  public void testGetName(ServiceMetricEvent serviceMetricEvent, String expectedPath)
  {
    TimelineMetric metric = defaultWhiteListBasedDruidToTimelineEventConverter.druidEventToTimelineMetric(serviceMetricEvent);
    String path = null;
    if (metric != null) {
      path = metric.getMetricName();
    }
    Assert.assertEquals(expectedPath, path);
  }

  @Test
  public void testWhiteListedStringArrayDimension() throws IOException
  {
    File mapFile = File.createTempFile("testing-" + System.nanoTime(), ".json");
    mapFile.deleteOnExit();

    try (OutputStream outputStream = new FileOutputStream(mapFile)) {
      IOUtils.copyLarge(
          getClass().getResourceAsStream("/testWhiteListedStringArrayDimension.json"),
          outputStream
      );
    }

    WhiteListBasedDruidToTimelineEventConverter converter = new WhiteListBasedDruidToTimelineEventConverter(
        prefix,
        "druid",
        mapFile.getAbsolutePath(),
        new DefaultObjectMapper()
    );

    ServiceMetricEvent event = new ServiceMetricEvent.Builder()
        .setDimension("gcName", new String[] {"g1"})
        .setMetric("jvm/gc/cpu", 10)
        .build(serviceName, hostname);

    TimelineMetric metric = converter.druidEventToTimelineMetric(event);

    Assert.assertNotNull(metric);
    Assert.assertEquals(defaultNamespace + ".g1.jvm/gc/cpu", metric.getMetricName());
  }

  @UsedByJUnitParamsRunner
  private Object[] parametersForTestGetName()
  {
    return new Object[]{
        new Object[]{
            new ServiceMetricEvent.Builder().setDimension("id", "dummy_id")
                                            .setDimension("status", "some_status")
                                            .setDimension("numDimensions", "1")
                                            .setDimension("segment", "dummy_segment")
                                            .setMetric("query/segment/time/balabla/more", 10)
                .build(serviceName, hostname),
            defaultNamespace + ".query/segment/time/balabla/more"
        },
        new Object[]{
            new ServiceMetricEvent.Builder().setDimension("dataSource", "some_data_source")
                                            .setDimension("tier", "_default_tier")
                                            .setMetric("segment/max", 10)
                .build(serviceName, hostname),
            null
        },
        new Object[]{
            new ServiceMetricEvent.Builder().setDimension("dataSource", "data-source")
                                            .setDimension("type", "groupBy")
                                            .setDimension("interval", "2013/2015")
                                            .setDimension("some_random_dim1", "random_dim_value1")
                                            .setDimension("some_random_dim2", "random_dim_value2")
                                            .setDimension("hasFilters", "no")
                                            .setDimension("duration", "P1D")
                                            .setDimension("remoteAddress", "194.0.90.2")
                                            .setDimension("id", "ID")
                                            .setDimension("context", "{context}")
                                            .setMetric("query/time", 10)
                .build(serviceName, hostname),
            defaultNamespace + ".data-source.groupBy.query/time"
        },
        new Object[]{
            new ServiceMetricEvent.Builder().setDimension("dataSource", "data-source")
                                            .setDimension("type", "groupBy")
                                            .setDimension("some_random_dim1", "random_dim_value1")
                                            .setMetric("ingest/persists/count", 10)
                .build(serviceName, hostname),
            defaultNamespace + ".data-source.ingest/persists/count"
        },
        new Object[]{
            new ServiceMetricEvent.Builder().setDimension("bufferpoolName", "BufferPool")
                                            .setDimension("type", "groupBy")
                                            .setDimension("some_random_dim1", "random_dim_value1")
                                            .setMetric("jvm/bufferpool/capacity", 10)
                .build(serviceName, hostname),
            null
        }
    };
  }
}
