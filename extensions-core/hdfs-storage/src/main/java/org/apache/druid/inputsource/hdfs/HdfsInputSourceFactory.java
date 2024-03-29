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

package org.apache.druid.inputsource.hdfs;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.druid.data.input.InputSourceFactory;
import org.apache.druid.data.input.impl.SplittableInputSource;
import org.apache.druid.data.input.impl.systemfield.SystemFields;
import org.apache.druid.guice.Hdfs;
import org.apache.hadoop.conf.Configuration;

import java.util.List;

public class HdfsInputSourceFactory implements InputSourceFactory
{
  private final Configuration configuration;
  private final HdfsInputSourceConfig inputSourceConfig;

  @JsonCreator
  public HdfsInputSourceFactory(
      @JacksonInject @Hdfs Configuration configuration,
      @JacksonInject HdfsInputSourceConfig inputSourceConfig
  )
  {
    this.configuration = configuration;
    this.inputSourceConfig = inputSourceConfig;
  }

  @Override
  public SplittableInputSource create(List<String> inputFilePaths)
  {
    return new HdfsInputSource(inputFilePaths, SystemFields.none(), configuration, inputSourceConfig);
  }
}
