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

package org.apache.druid.storage.google;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class GoogleByteSourceTest extends EasyMockSupport
{
  @Test
  public void openStreamTest() throws IOException
  {
    final String bucket = "bucket";
    final String path = "/path/to/file";
    GoogleStorage storage = createMock(GoogleStorage.class);
    InputStream stream = createMock(InputStream.class);

    EasyMock.expect(storage.getInputStream(bucket, path)).andReturn(stream);

    replayAll();

    GoogleByteSource byteSource = new GoogleByteSource(storage, bucket, path);

    byteSource.openStream();

    verifyAll();
  }

  @Test(expected = IOException.class)
  public void openStreamWithRecoverableErrorTest() throws IOException
  {
    final String bucket = "bucket";
    final String path = "/path/to/file";
    GoogleStorage storage = createMock(GoogleStorage.class);

    EasyMock.expect(storage.getInputStream(bucket, path)).andThrow(new IOException(""));

    replayAll();

    GoogleByteSource byteSource = new GoogleByteSource(storage, bucket, path);

    byteSource.openStream();

    verifyAll();
  }
}
