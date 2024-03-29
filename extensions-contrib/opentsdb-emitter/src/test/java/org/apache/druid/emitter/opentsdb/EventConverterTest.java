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

package org.apache.druid.emitter.opentsdb;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.druid.java.util.common.DateTimes;
import org.apache.druid.java.util.emitter.service.ServiceMetricEvent;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class EventConverterTest
{
  private EventConverter converterWithNamespacePrefix;
  private EventConverter converterWithNamespacePrefixContainingSpace;
  private EventConverter converterWithoutNamespacePrefix;

  @Before
  public void setUp()
  {
    converterWithNamespacePrefix = new EventConverter(new ObjectMapper(), null, "druid");
    converterWithNamespacePrefixContainingSpace = new EventConverter(new ObjectMapper(), null, "legendary druid");
    converterWithoutNamespacePrefix = new EventConverter(new ObjectMapper(), null, null);
  }

  @Test
  public void testSanitize()
  {
    String metric = " foo bar/baz";
    Assert.assertEquals("foo_bar.baz", converterWithNamespacePrefix.sanitize(metric));
    Assert.assertEquals("foo_bar.baz", converterWithNamespacePrefixContainingSpace.sanitize(metric));
    Assert.assertEquals("foo_bar.baz", converterWithoutNamespacePrefix.sanitize(metric));
  }

  @Test
  public void testConvertWithNamespacePrefix()
  {
    DateTime dateTime = DateTimes.nowUtc();
    ServiceMetricEvent configuredEvent = new ServiceMetricEvent.Builder()
        .setDimension("dataSource", "foo:bar")
        .setDimension("type", "groupBy")
        .setCreatedTime(dateTime)
        .setMetric("query/time", 10)
        .build("druid:broker", "127.0.0.1:8080");

    Map<String, Object> expectedTags = new HashMap<>();
    expectedTags.put("service", "druid_broker");
    expectedTags.put("host", "127.0.0.1_8080");
    expectedTags.put("dataSource", "foo_bar");
    expectedTags.put("type", "groupBy");

    OpentsdbEvent opentsdbEvent = converterWithNamespacePrefix.convert(configuredEvent);
    Assert.assertEquals("druid.query.time", opentsdbEvent.getMetric());
    Assert.assertEquals(dateTime.getMillis() / 1000L, opentsdbEvent.getTimestamp());
    Assert.assertEquals(10, opentsdbEvent.getValue());
    Assert.assertEquals(expectedTags, opentsdbEvent.getTags());

    ServiceMetricEvent notConfiguredEvent = new ServiceMetricEvent.Builder()
        .setDimension("dataSource", "data-source")
        .setDimension("type", "groupBy")
        .setCreatedTime(dateTime)
        .setMetric("foo/bar", 10)
        .build("broker", "brokerHost1");
    Assert.assertNull(converterWithNamespacePrefix.convert(notConfiguredEvent));
  }

  @Test
  public void testConvertWithNamespacePrefixContainingSpace()
  {
    DateTime dateTime = DateTimes.nowUtc();
    ServiceMetricEvent configuredEvent = new ServiceMetricEvent.Builder()
        .setDimension("dataSource", "foo:bar")
        .setDimension("type", "groupBy")
        .setCreatedTime(dateTime)
        .setMetric("query/time", 10)
        .build("druid:broker", "127.0.0.1:8080");

    Map<String, Object> expectedTags = new HashMap<>();
    expectedTags.put("service", "druid_broker");
    expectedTags.put("host", "127.0.0.1_8080");
    expectedTags.put("dataSource", "foo_bar");
    expectedTags.put("type", "groupBy");

    OpentsdbEvent opentsdbEvent = converterWithNamespacePrefixContainingSpace.convert(configuredEvent);
    Assert.assertEquals("legendary_druid.query.time", opentsdbEvent.getMetric());
    Assert.assertEquals(dateTime.getMillis() / 1000L, opentsdbEvent.getTimestamp());
    Assert.assertEquals(10, opentsdbEvent.getValue());
    Assert.assertEquals(expectedTags, opentsdbEvent.getTags());

    ServiceMetricEvent notConfiguredEvent = new ServiceMetricEvent.Builder()
        .setDimension("dataSource", "data-source")
        .setDimension("type", "groupBy")
        .setCreatedTime(dateTime)
        .setMetric("foo/bar", 10)
        .build("broker", "brokerHost1");
    Assert.assertNull(converterWithNamespacePrefixContainingSpace.convert(notConfiguredEvent));
  }

  @Test
  public void testConvertWithoutNamespacePrefix()
  {
    DateTime dateTime = DateTimes.nowUtc();
    ServiceMetricEvent configuredEvent = new ServiceMetricEvent.Builder()
        .setDimension("dataSource", "foo:bar")
        .setDimension("type", "groupBy")
        .setCreatedTime(dateTime)
        .setMetric("query/time", 10)
        .build("druid:broker", "127.0.0.1:8080");

    Map<String, Object> expectedTags = new HashMap<>();
    expectedTags.put("service", "druid_broker");
    expectedTags.put("host", "127.0.0.1_8080");
    expectedTags.put("dataSource", "foo_bar");
    expectedTags.put("type", "groupBy");

    OpentsdbEvent opentsdbEvent = converterWithoutNamespacePrefix.convert(configuredEvent);
    Assert.assertEquals("query.time", opentsdbEvent.getMetric());
    Assert.assertEquals(dateTime.getMillis() / 1000L, opentsdbEvent.getTimestamp());
    Assert.assertEquals(10, opentsdbEvent.getValue());
    Assert.assertEquals(expectedTags, opentsdbEvent.getTags());

    ServiceMetricEvent notConfiguredEvent = new ServiceMetricEvent.Builder()
        .setDimension("dataSource", "data-source")
        .setDimension("type", "groupBy")
        .setCreatedTime(dateTime)
        .setMetric("foo/bar", 10)
        .build("broker", "brokerHost1");
    Assert.assertNull(converterWithoutNamespacePrefix.convert(notConfiguredEvent));
  }

}
