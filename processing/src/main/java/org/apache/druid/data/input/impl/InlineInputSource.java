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

package org.apache.druid.data.input.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.druid.data.input.AbstractInputSource;
import org.apache.druid.data.input.InputFormat;
import org.apache.druid.data.input.InputRowSchema;
import org.apache.druid.data.input.InputSourceReader;
import org.apache.druid.data.input.impl.systemfield.SystemFieldDecoratorFactory;
import org.apache.druid.java.util.common.CloseableIterators;
import org.apache.druid.java.util.common.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class InlineInputSource extends AbstractInputSource
{
  public static final String TYPE_KEY = "inline";

  private final String data;

  @JsonCreator
  public InlineInputSource(@JsonProperty("data") String data)
  {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(data), "empty data");
    this.data = data;
  }

  @JsonIgnore
  @Nonnull
  @Override
  public Set<String> getTypes()
  {
    return Collections.singleton(TYPE_KEY);
  }

  @JsonProperty
  public String getData()
  {
    return data;
  }

  @Override
  @JsonIgnore
  public boolean isSplittable()
  {
    return false;
  }

  @Override
  public boolean needsFormat()
  {
    return true;
  }

  @Override
  protected InputSourceReader formattableReader(
      InputRowSchema inputRowSchema,
      InputFormat inputFormat,
      @Nullable File temporaryDirectory
  )
  {
    return new InputEntityIteratingReader(
        inputRowSchema,
        inputFormat,
        CloseableIterators.withEmptyBaggage(Stream.of(new ByteEntity(StringUtils.toUtf8(data))).iterator()),
        SystemFieldDecoratorFactory.NONE,
        temporaryDirectory
    );
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
    InlineInputSource that = (InlineInputSource) o;
    return Objects.equals(data, that.data);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(data);
  }

  @Override
  public String toString()
  {
    return "InlineInputSource{" +
           "data='" + data + '\'' +
           '}';
  }
}
