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

package org.apache.druid.query.aggregation;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Supplier;
import org.apache.druid.math.expr.ExprMacroTable;
import org.apache.druid.segment.BaseLongColumnValueSelector;
import org.apache.druid.segment.vector.VectorColumnSelectorFactory;
import org.apache.druid.segment.vector.VectorValueSelector;

import javax.annotation.Nullable;

/**
 */
public class LongMinAggregatorFactory extends SimpleLongAggregatorFactory
{
  private final Supplier<byte[]> cacheKey;

  @JsonCreator
  public LongMinAggregatorFactory(
      @JsonProperty("name") String name,
      @JsonProperty("fieldName") final String fieldName,
      @JsonProperty("expression") @Nullable String expression,
      @JacksonInject ExprMacroTable macroTable
  )
  {
    super(macroTable, name, fieldName, expression);
    this.cacheKey = AggregatorUtil.getSimpleAggregatorCacheKeySupplier(
        AggregatorUtil.LONG_MIN_CACHE_TYPE_ID,
        fieldName,
        fieldExpression
    );
  }

  public LongMinAggregatorFactory(String name, String fieldName)
  {
    this(name, fieldName, null, ExprMacroTable.nil());
  }

  @Override
  protected long nullValue()
  {
    return Long.MAX_VALUE;
  }

  @Override
  protected Aggregator buildAggregator(BaseLongColumnValueSelector selector)
  {
    return new LongMinAggregator(selector);
  }

  @Override
  protected BufferAggregator buildBufferAggregator(BaseLongColumnValueSelector selector)
  {
    return new LongMinBufferAggregator(selector);
  }

  @Override
  protected VectorAggregator factorizeVector(
          VectorColumnSelectorFactory columnSelectorFactory,
          VectorValueSelector selector
  )
  {
    return new LongMinVectorAggregator(selector);
  }

  @Override
  @Nullable
  public Object combine(@Nullable Object lhs, @Nullable Object rhs)
  {
    if (rhs == null) {
      return lhs;
    }
    if (lhs == null) {
      return rhs;
    }
    return LongMinAggregator.combineValues(lhs, rhs);
  }

  @Override
  public AggregateCombiner makeAggregateCombiner()
  {
    return new LongMinAggregateCombiner();
  }

  @Override
  public AggregatorFactory getCombiningFactory()
  {
    return new LongMinAggregatorFactory(name, name, null, macroTable);
  }

  @Override
  public AggregatorFactory withName(String newName)
  {
    return new LongMinAggregatorFactory(newName, getFieldName(), getExpression(), macroTable);
  }

  @Override
  public byte[] getCacheKey()
  {
    return cacheKey.get();
  }

  @Override
  public String toString()
  {
    return "LongMinAggregatorFactory{" +
           "fieldName='" + fieldName + '\'' +
           ", expression='" + expression + '\'' +
           ", name='" + name + '\'' +
           '}';
  }
}
