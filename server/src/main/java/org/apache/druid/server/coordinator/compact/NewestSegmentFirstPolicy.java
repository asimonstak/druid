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

package org.apache.druid.server.coordinator.compact;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.apache.druid.server.coordinator.DataSourceCompactionConfig;
import org.apache.druid.timeline.SegmentTimeline;
import org.joda.time.Interval;

import java.util.List;
import java.util.Map;

/**
 * This policy searches segments for compaction from the newest one to oldest one.
 */
public class NewestSegmentFirstPolicy implements CompactionSegmentSearchPolicy
{
  private final ObjectMapper objectMapper;

  @Inject
  public NewestSegmentFirstPolicy(ObjectMapper objectMapper)
  {
    this.objectMapper = objectMapper;
  }

  @Override
  public CompactionSegmentIterator reset(
      Map<String, DataSourceCompactionConfig> compactionConfigs,
      Map<String, SegmentTimeline> dataSources,
      Map<String, List<Interval>> skipIntervals
  )
  {
    return new NewestSegmentFirstIterator(objectMapper, compactionConfigs, dataSources, skipIntervals);
  }
}
