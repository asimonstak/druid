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

package org.apache.druid.query.filter.vector;

import org.apache.druid.query.filter.DruidPredicateFactory;
import org.apache.druid.segment.column.ColumnType;

import javax.annotation.Nullable;

public interface VectorValueMatcherFactory
{
  /**
   * Specialized value matcher for string equality used by {@link org.apache.druid.query.filter.SelectorDimFilter}
   */
  VectorValueMatcher makeMatcher(@Nullable String value);

  /**
   * Specialized value matcher for equality used by {@link org.apache.druid.query.filter.EqualityFilter}. The
   * matchValue parameter must be the appropriate Java type for the matchValueType {@link ColumnType}. Implementors can
   * use this information to coerce the match value to the native type of the values to match against as necessary.
   */
  VectorValueMatcher makeMatcher(Object matchValue, ColumnType matchValueType);

  VectorValueMatcher makeMatcher(DruidPredicateFactory predicateFactory);
}
