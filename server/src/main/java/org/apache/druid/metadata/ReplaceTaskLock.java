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

package org.apache.druid.metadata;

import org.joda.time.Interval;

import java.util.Objects;

/**
 * Details of a REPLACE lock held by a batch supervisor task.
 * <p>
 * Replace locks are always held by the supervisor task, i.e. ParallelIndexSupervisorTask
 * in case of native batch ingestion and ControllerTask in case of MSQ ingestion.
 */
public class ReplaceTaskLock
{
  private final String supervisorTaskId;
  private final Interval interval;
  private final String version;

  public ReplaceTaskLock(String supervisorTaskId, Interval interval, String version)
  {
    this.supervisorTaskId = supervisorTaskId;
    this.interval = interval;
    this.version = version;
  }

  public String getSupervisorTaskId()
  {
    return supervisorTaskId;
  }

  public Interval getInterval()
  {
    return interval;
  }

  public String getVersion()
  {
    return version;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReplaceTaskLock that = (ReplaceTaskLock) o;
    return Objects.equals(supervisorTaskId, that.supervisorTaskId)
           && Objects.equals(interval, that.interval)
           && Objects.equals(version, that.version);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(supervisorTaskId, interval, version);
  }
}
