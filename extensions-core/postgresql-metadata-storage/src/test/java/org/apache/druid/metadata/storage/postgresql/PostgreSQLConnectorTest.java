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

package org.apache.druid.metadata.storage.postgresql;

import com.google.common.base.Suppliers;
import org.apache.druid.metadata.MetadataStorageConnectorConfig;
import org.apache.druid.metadata.MetadataStorageTablesConfig;
import org.junit.Assert;
import org.junit.Test;

import java.sql.SQLException;

public class PostgreSQLConnectorTest
{

  @Test
  public void testIsTransientException()
  {
    PostgreSQLConnector connector = new PostgreSQLConnector(
        Suppliers.ofInstance(new MetadataStorageConnectorConfig()),
        Suppliers.ofInstance(MetadataStorageTablesConfig.fromBase(null)),
        new PostgreSQLConnectorConfig(),
        new PostgreSQLTablesConfig()
    );

    Assert.assertTrue(connector.isTransientException(new SQLException("bummer, connection problem", "08DIE")));
    Assert.assertTrue(connector.isTransientException(new SQLException("bummer, too many things going on", "53RES")));
    Assert.assertFalse(connector.isTransientException(new SQLException("oh god, no!", "58000")));
    Assert.assertFalse(connector.isTransientException(new SQLException("help!")));
    Assert.assertFalse(connector.isTransientException(new SQLException()));
    Assert.assertFalse(connector.isTransientException(new Exception("I'm not happy")));
    Assert.assertFalse(connector.isTransientException(new Throwable("I give up")));
  }

  @Test
  public void testLimitClause()
  {
    PostgreSQLConnector connector = new PostgreSQLConnector(
        Suppliers.ofInstance(new MetadataStorageConnectorConfig()),
        Suppliers.ofInstance(MetadataStorageTablesConfig.fromBase(null)),
        new PostgreSQLConnectorConfig(),
        new PostgreSQLTablesConfig()
    );
    Assert.assertEquals("LIMIT 100", connector.limitClause(100));
  }
}
