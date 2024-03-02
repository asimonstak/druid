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

package org.apache.druid.segment.transform;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import org.apache.druid.data.input.InputSourceReader;
import org.apache.druid.data.input.impl.InputRowParser;
import org.apache.druid.data.input.impl.StringInputRowParser;
import org.apache.druid.java.util.common.ISE;
import org.apache.druid.query.filter.DimFilter;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Specifies how input rows should be filtered and transforms. There are two parts: a "filter" (which can filter out
 * input rows) and "transforms" (which can add fields to input rows). Filters may refer to fields generated by
 * a transform.
 *
 * See {@link Transform} for details on how each transform works.
 */
public class TransformSpec
{
  public static final TransformSpec NONE = new TransformSpec(null, null);

  private final DimFilter filter;
  private final List<Transform> transforms;

  @JsonCreator
  public TransformSpec(
      @JsonProperty("filter") final DimFilter filter,
      @JsonProperty("transforms") final List<Transform> transforms
  )
  {
    this.filter = filter;
    this.transforms = transforms == null ? ImmutableList.of() : transforms;

    // Check for name collisions.
    final Set<String> seen = new HashSet<>();
    for (Transform transform : this.transforms) {
      if (!seen.add(transform.getName())) {
        throw new ISE("Transform name '%s' cannot be used twice", transform.getName());
      }
    }
  }

  @JsonProperty
  @Nullable
  public DimFilter getFilter()
  {
    return filter;
  }

  @JsonProperty
  public List<Transform> getTransforms()
  {
    return transforms;
  }

  public <T> InputRowParser<T> decorate(final InputRowParser<T> parser)
  {
    // Always decorates, even if the transformSpec is a no-op. This is so fromInputRowParser can insist that the
    // parser is a transforming parser, and possibly help detect coding errors where someone forgot to call "decorate".

    if (parser instanceof StringInputRowParser) {
      // Hack to support the fact that some callers use special methods in StringInputRowParser, such as
      // parse(String) and startFileFromBeginning.
      return (InputRowParser<T>) new TransformingStringInputRowParser(
          parser.getParseSpec(),
          ((StringInputRowParser) parser).getEncoding(),
          this
      );
    } else {
      return new TransformingInputRowParser<>(parser, this);
    }
  }

  public InputSourceReader decorate(InputSourceReader reader)
  {
    return new TransformingInputSourceReader(reader, toTransformer());
  }

  /**
   * Create a {@link Transformer} from this TransformSpec, when the rows to be transformed do not have a known
   * signature.
   */
  public Transformer toTransformer()
  {
    return new Transformer(this);
  }

  public Set<String> getRequiredColumns()
  {
    final Set<String> requiredColumns = new HashSet<>();

    if (filter != null) {
      requiredColumns.addAll(filter.getRequiredColumns());
    }

    for (Transform transform : transforms) {
      requiredColumns.addAll(transform.getRequiredColumns());
    }

    return requiredColumns;
  }

  @Override
  public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final TransformSpec that = (TransformSpec) o;
    return Objects.equals(filter, that.filter) &&
           Objects.equals(transforms, that.transforms);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(filter, transforms);
  }

  @Override
  public String toString()
  {
    return "TransformSpec{" +
           "filter=" + filter +
           ", transforms=" + transforms +
           '}';
  }
}