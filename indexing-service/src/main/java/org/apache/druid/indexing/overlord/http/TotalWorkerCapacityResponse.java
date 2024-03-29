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

package org.apache.druid.indexing.overlord.http;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Should be synchronized with org.apache.druid.client.indexing.IndexingTotalWorkerCapacityInfo
 */
public class TotalWorkerCapacityResponse
{
  /**
   * The total worker capacity of the current state of the cluster. This can be -1 if
   * it cannot be determined.
   */
  private final int currentClusterCapacity;
  /**
   * The total worker capacity of the cluster including auto scaling capability (scaling to max workers).
   * This can be -1 if it cannot be determined or if auto scaling is not configured.
   */
  private final int maximumCapacityWithAutoScale;
  /**
   * Used cluster capacity of the current state of the cluster. This can be -1 if
   * it cannot be determined.
   */
  private final int usedClusterCapacity;

  @JsonCreator
  public TotalWorkerCapacityResponse(
      @JsonProperty("currentClusterCapacity") int currentClusterCapacity,
      @JsonProperty("maximumCapacityWithAutoScale") int maximumCapacityWithAutoScale,
      @JsonProperty("usedClusterCapacity") int usedClusterCapacity
  )
  {
    this.currentClusterCapacity = currentClusterCapacity;
    this.maximumCapacityWithAutoScale = maximumCapacityWithAutoScale;
    this.usedClusterCapacity = usedClusterCapacity;
  }

  @JsonProperty
  public int getCurrentClusterCapacity()
  {
    return currentClusterCapacity;
  }

  @JsonProperty
  public int getMaximumCapacityWithAutoScale()
  {
    return maximumCapacityWithAutoScale;
  }

  @JsonProperty
  public int getUsedClusterCapacity()
  {
    return usedClusterCapacity;
  }
}
