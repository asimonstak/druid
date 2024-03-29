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

package org.apache.druid.java.util.common;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Test;

import java.util.TimeZone;

public class DateTimesTest
{
  @Test
  public void testCommonDateTimePattern()
  {
    DateTime dt1 = DateTimes.nowUtc();
    DateTime dt2 = new DateTime(System.currentTimeMillis(), DateTimes.inferTzFromString("IST"));
    DateTime dt3 = new DateTime(System.currentTimeMillis(), DateTimeZone.forOffsetHoursMinutes(1, 30));

    for (DateTime dt : new DateTime[]{dt1, dt2, dt3}) {
      Assert.assertTrue(DateTimes.COMMON_DATE_TIME_PATTERN.matcher(dt.toString()).matches());
    }
  }

  @Test
  public void testinferTzFromStringWithKnownTzId()
  {
    Assert.assertEquals(DateTimeZone.UTC, DateTimes.inferTzFromString("UTC"));
  }

  @Test
  public void testinferTzFromStringWithOffset()
  {
    Assert.assertEquals(DateTimeZone.forOffsetHoursMinutes(10, 30), DateTimes.inferTzFromString("+1030"));
  }

  @Test
  public void testinferTzFromStringWithJavaTimeZone()
  {
    Assert.assertEquals(
        DateTimeZone.forTimeZone(TimeZone.getTimeZone("ACT")),
        DateTimes.inferTzFromString("ACT")
    );
  }

  @Test
  public void testinferTzFromStringWithJavaTimeZoneAndNoFallback()
  {
    Assert.assertEquals(
        DateTimeZone.forTimeZone(TimeZone.getTimeZone("ACT")),
        DateTimes.inferTzFromString("ACT", false)
    );
  }

  @Test
  public void testinferTzFromStringWithUnknownTimeZoneShouldReturnUTC()
  {
    Assert.assertEquals(DateTimeZone.UTC, DateTimes.inferTzFromString("America/Unknown"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testinferTzFromStringWithUnknownTimeZoneAndNoFallbackShouldThrowException()
  {
    Assert.assertEquals(DateTimeZone.getDefault(), DateTimes.inferTzFromString("America/Unknown", false));
  }

  @Test
  public void testStringToDateTimeConversion()
  {
    String seconds = "2018-01-30T06:00:00";
    DateTime dt2 = DateTimes.of(seconds);
    Assert.assertEquals("2018-01-30T06:00:00.000Z", dt2.toString());

    String milis = "1517292000000";
    DateTime dt1 = DateTimes.of(milis);
    Assert.assertEquals("2018-01-30T06:00:00.000Z", dt1.toString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testStringToDateTimeConverstion_RethrowInitialException()
  {
    String invalid = "51729200AZ";
    DateTimes.of(invalid);
  }

  @Test
  public void testCanCompareAsString()
  {
    Assert.assertTrue(DateTimes.canCompareAsString(DateTimes.EPOCH));
    Assert.assertTrue(DateTimes.canCompareAsString(DateTimes.of("0000-01-01")));

    Assert.assertEquals("0000-01-01T00:00:00.000Z", DateTimes.COMPARE_DATE_AS_STRING_MIN.toString());
    Assert.assertEquals("9999-12-31T23:59:59.999Z", DateTimes.COMPARE_DATE_AS_STRING_MAX.toString());

    Assert.assertTrue(DateTimes.canCompareAsString(DateTimes.of("9999")));
    Assert.assertTrue(DateTimes.canCompareAsString(DateTimes.of("2000")));

    Assert.assertFalse(DateTimes.canCompareAsString(DateTimes.MIN));
    Assert.assertFalse(DateTimes.canCompareAsString(DateTimes.MAX));
    Assert.assertFalse(DateTimes.canCompareAsString(DateTimes.of("-1-01-01T00:00:00")));
    Assert.assertFalse(DateTimes.canCompareAsString(DateTimes.of("10000-01-01")));

    // Can't compare as string with mixed time zones.
    Assert.assertFalse(DateTimes.canCompareAsString(
        DateTimes.of("2000").withZone(DateTimes.inferTzFromString("America/Los_Angeles")))
    );
  }

  @Test
  public void testEarlierOf()
  {
    Assert.assertNull(DateTimes.earlierOf(null, null));

    final DateTime jan14 = DateTimes.of("2013-01-14");
    Assert.assertEquals(jan14, DateTimes.earlierOf(null, jan14));
    Assert.assertEquals(jan14, DateTimes.earlierOf(jan14, null));
    Assert.assertEquals(jan14, DateTimes.earlierOf(jan14, jan14));

    final DateTime jan15 = DateTimes.of("2013-01-15");
    Assert.assertEquals(jan14, DateTimes.earlierOf(jan15, jan14));
    Assert.assertEquals(jan14, DateTimes.earlierOf(jan14, jan15));
  }

  @Test
  public void testLaterOf()
  {
    Assert.assertNull(DateTimes.laterOf(null, null));

    final DateTime jan14 = DateTimes.of("2013-01-14");
    Assert.assertEquals(jan14, DateTimes.laterOf(null, jan14));
    Assert.assertEquals(jan14, DateTimes.laterOf(jan14, null));
    Assert.assertEquals(jan14, DateTimes.laterOf(jan14, jan14));

    final DateTime jan15 = DateTimes.of("2013-01-15");
    Assert.assertEquals(jan15, DateTimes.laterOf(jan15, jan14));
    Assert.assertEquals(jan15, DateTimes.laterOf(jan14, jan15));
  }
}
