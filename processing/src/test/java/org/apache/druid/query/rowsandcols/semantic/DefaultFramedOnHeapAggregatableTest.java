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

package org.apache.druid.query.rowsandcols.semantic;

import org.junit.Test;

import static org.apache.druid.query.rowsandcols.semantic.DefaultFramedOnHeapAggregatable.invertedOrderForLastK;
import static org.junit.Assert.assertEquals;

public class DefaultFramedOnHeapAggregatableTest
{
  @Test
  public void testInvertedOrderForLastK()
  {
    assertEquals(0, invertedOrderForLastK(0, 3, 1));
    assertEquals(1, invertedOrderForLastK(1, 3, 1));
    assertEquals(2, invertedOrderForLastK(2, 3, 1));
  }

  @Test
  public void testInvertedOrderForLastK2()
  {
    assertEquals(0, invertedOrderForLastK(0, 3, 2));
    assertEquals(2, invertedOrderForLastK(1, 3, 2));
    assertEquals(1, invertedOrderForLastK(2, 3, 2));
  }

  @Test
  public void testInvertedOrderForLastK3()
  {
    assertEquals(2, invertedOrderForLastK(0, 3, 3));
    assertEquals(1, invertedOrderForLastK(1, 3, 3));
    assertEquals(0, invertedOrderForLastK(2, 3, 3));
  }
}
