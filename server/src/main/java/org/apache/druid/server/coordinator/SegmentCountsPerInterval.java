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

package org.apache.druid.server.coordinator;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.druid.timeline.DataSegment;
import org.joda.time.Interval;

import java.util.HashMap;
import java.util.Map;

/**
 * Maintains a count of segments for each datasource and interval.
 */
public class SegmentCountsPerInterval
{
  private int totalSegments;
  private long totalSegmentBytes;
  private final Map<String, Object2IntMap<Interval>> datasourceIntervalToSegmentCount = new HashMap<>();
  private final Object2IntMap<Interval> intervalToTotalSegmentCount = new Object2IntOpenHashMap<>();
  private final Object2IntMap<String> datasourceToTotalSegmentCount = new Object2IntOpenHashMap<>();

  public void addSegment(DataSegment segment)
  {
    updateCountInInterval(segment, 1);
    totalSegmentBytes += segment.getSize();
  }

  public void removeSegment(DataSegment segment)
  {
    updateCountInInterval(segment, -1);
    totalSegmentBytes -= segment.getSize();
  }

  public int getTotalSegmentCount()
  {
    return totalSegments;
  }

  public long getTotalSegmentBytes()
  {
    return totalSegmentBytes;
  }

  public Object2IntMap<String> getDatasourceToTotalSegmentCount()
  {
    return datasourceToTotalSegmentCount;
  }

  public Object2IntMap<Interval> getIntervalToSegmentCount(String datasource)
  {
    return datasourceIntervalToSegmentCount.getOrDefault(datasource, Object2IntMaps.emptyMap());
  }

  public Object2IntMap<Interval> getIntervalToTotalSegmentCount()
  {
    return intervalToTotalSegmentCount;
  }

  private void updateCountInInterval(DataSegment segment, int delta)
  {
    totalSegments += delta;
    intervalToTotalSegmentCount.mergeInt(segment.getInterval(), delta, Integer::sum);
    datasourceToTotalSegmentCount.mergeInt(segment.getDataSource(), delta, Integer::sum);
    datasourceIntervalToSegmentCount
        .computeIfAbsent(segment.getDataSource(), ds -> new Object2IntOpenHashMap<>())
        .mergeInt(segment.getInterval(), delta, Integer::sum);
  }
}
