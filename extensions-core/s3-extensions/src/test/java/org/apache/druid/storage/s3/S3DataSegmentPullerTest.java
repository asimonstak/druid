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

package org.apache.druid.storage.s3;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.apache.commons.io.IOUtils;
import org.apache.druid.data.input.impl.CloudObjectLocation;
import org.apache.druid.java.util.common.FileUtils;
import org.apache.druid.java.util.common.StringUtils;
import org.apache.druid.segment.loading.SegmentLoadingException;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

/**
 *
 */
public class S3DataSegmentPullerTest
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testSimpleGetVersion() throws IOException
  {
    String bucket = "bucket";
    String keyPrefix = "prefix/dir/0";
    String expectedKey = keyPrefix + "/renames-0.gz";
    ServerSideEncryptingAmazonS3 s3Client = EasyMock.createStrictMock(ServerSideEncryptingAmazonS3.class);

    final ObjectMetadata objectMetadata = new ObjectMetadata();
    objectMetadata.setLastModified(new Date(0));

    EasyMock.expect(s3Client.getObjectMetadata(bucket, expectedKey))
            .andReturn(objectMetadata)
            .once();
    S3DataSegmentPuller puller = new S3DataSegmentPuller(s3Client);

    EasyMock.replay(s3Client);

    String version = puller.getVersion(URI.create(StringUtils.format("s3://%s/%s", bucket, expectedKey)));

    EasyMock.verify(s3Client);

    Assert.assertEquals(StringUtils.format("%d", new Date(0).getTime()), version);
  }

  @Test
  public void testGZUncompress() throws IOException, SegmentLoadingException
  {
    final String bucket = "bucket";
    final String keyPrefix = "prefix/dir/0";
    final ServerSideEncryptingAmazonS3 s3Client = EasyMock.createStrictMock(ServerSideEncryptingAmazonS3.class);
    final byte[] value = bucket.getBytes(StandardCharsets.UTF_8);

    final File tmpFile = temporaryFolder.newFile("gzTest.gz");

    try (OutputStream outputStream = new GZIPOutputStream(new FileOutputStream(tmpFile))) {
      outputStream.write(value);
    }

    final S3Object object0 = new S3Object();
    object0.setBucketName(bucket);
    object0.setKey(keyPrefix + "/renames-0.gz");
    object0.getObjectMetadata().setLastModified(new Date(0));
    object0.setObjectContent(new FileInputStream(tmpFile));

    final S3ObjectSummary objectSummary = new S3ObjectSummary();
    objectSummary.setBucketName(bucket);
    objectSummary.setKey(keyPrefix + "/renames-0.gz");
    objectSummary.setLastModified(new Date(0));

    final File tmpDir = temporaryFolder.newFolder("gzTestDir");

    EasyMock.expect(s3Client.doesObjectExist(EasyMock.eq(object0.getBucketName()), EasyMock.eq(object0.getKey())))
            .andReturn(true)
            .once();
    EasyMock.expect(s3Client.getObject(EasyMock.eq(object0.getBucketName()), EasyMock.eq(object0.getKey())))
            .andReturn(object0)
            .once();
    S3DataSegmentPuller puller = new S3DataSegmentPuller(s3Client);

    EasyMock.replay(s3Client);
    FileUtils.FileCopyResult result = puller.getSegmentFiles(
        new CloudObjectLocation(
            bucket,
            object0.getKey()
        ), tmpDir
    );
    EasyMock.verify(s3Client);

    Assert.assertEquals(value.length, result.size());
    File expected = new File(tmpDir, "renames-0");
    Assert.assertTrue(expected.exists());
    Assert.assertEquals(value.length, expected.length());
  }

  @Test
  public void testGZUncompressOn4xxError() throws IOException
  {
    final String bucket = "bucket";
    final String keyPrefix = "prefix/dir/0";
    final ServerSideEncryptingAmazonS3 s3Client = EasyMock.createStrictMock(ServerSideEncryptingAmazonS3.class);
    final byte[] value = bucket.getBytes(StandardCharsets.UTF_8);

    final File tmpFile = temporaryFolder.newFile("gzTest.gz");

    try (OutputStream outputStream = new GZIPOutputStream(new FileOutputStream(tmpFile))) {
      outputStream.write(value);
    }

    S3Object object0 = new S3Object();

    object0.setBucketName(bucket);
    object0.setKey(keyPrefix + "/renames-0.gz");
    object0.getObjectMetadata().setLastModified(new Date(0));
    object0.setObjectContent(new FileInputStream(tmpFile));

    final S3ObjectSummary objectSummary = new S3ObjectSummary();
    objectSummary.setBucketName(bucket);
    objectSummary.setKey(keyPrefix + "/renames-0.gz");
    objectSummary.setLastModified(new Date(0));

    final ListObjectsV2Result listObjectsResult = new ListObjectsV2Result();
    listObjectsResult.setKeyCount(1);
    listObjectsResult.getObjectSummaries().add(objectSummary);

    File tmpDir = temporaryFolder.newFolder("gzTestDir");

    AmazonS3Exception exception = new AmazonS3Exception("S3DataSegmentPullerTest");
    exception.setErrorCode("NoSuchKey");
    exception.setStatusCode(404);
    EasyMock.expect(s3Client.doesObjectExist(EasyMock.eq(object0.getBucketName()), EasyMock.eq(object0.getKey())))
            .andReturn(true)
            .once();
    EasyMock.expect(s3Client.getObject(EasyMock.eq(bucket), EasyMock.eq(object0.getKey())))
            .andThrow(exception)
            .once();
    S3DataSegmentPuller puller = new S3DataSegmentPuller(s3Client);

    EasyMock.replay(s3Client);
    Assert.assertThrows(
        SegmentLoadingException.class,
        () -> puller.getSegmentFiles(
            new CloudObjectLocation(
                bucket,
                object0.getKey()
            ), tmpDir
        )
    );
    EasyMock.verify(s3Client);

    File expected = new File(tmpDir, "renames-0");
    Assert.assertFalse(expected.exists());
  }

  @Test
  public void testGZUncompressOn5xxError() throws IOException, SegmentLoadingException
  {
    final String bucket = "bucket";
    final String keyPrefix = "prefix/dir/0";
    final ServerSideEncryptingAmazonS3 s3Client = EasyMock.createStrictMock(ServerSideEncryptingAmazonS3.class);
    final byte[] value = bucket.getBytes(StandardCharsets.UTF_8);

    final File tmpFile = temporaryFolder.newFile("gzTest.gz");

    try (OutputStream outputStream = new GZIPOutputStream(new FileOutputStream(tmpFile))) {
      outputStream.write(value);
    }

    S3Object object0 = new S3Object();

    object0.setBucketName(bucket);
    object0.setKey(keyPrefix + "/renames-0.gz");
    object0.getObjectMetadata().setLastModified(new Date(0));
    object0.setObjectContent(new FileInputStream(tmpFile));

    final S3ObjectSummary objectSummary = new S3ObjectSummary();
    objectSummary.setBucketName(bucket);
    objectSummary.setKey(keyPrefix + "/renames-0.gz");
    objectSummary.setLastModified(new Date(0));

    final ListObjectsV2Result listObjectsResult = new ListObjectsV2Result();
    listObjectsResult.setKeyCount(1);
    listObjectsResult.getObjectSummaries().add(objectSummary);

    File tmpDir = temporaryFolder.newFolder("gzTestDir");

    AmazonS3Exception exception = new AmazonS3Exception("S3DataSegmentPullerTest");
    exception.setErrorCode("Slow Down");
    exception.setStatusCode(503);
    EasyMock.expect(s3Client.doesObjectExist(EasyMock.eq(object0.getBucketName()), EasyMock.eq(object0.getKey())))
            .andReturn(true)
            .once();
    EasyMock.expect(s3Client.getObject(EasyMock.eq(bucket), EasyMock.eq(object0.getKey())))
            .andThrow(exception)
            .once();
    EasyMock.expect(s3Client.getObject(EasyMock.eq(bucket), EasyMock.eq(object0.getKey())))
            .andReturn(object0)
            .once();
    S3DataSegmentPuller puller = new S3DataSegmentPuller(s3Client);

    EasyMock.replay(s3Client);
    FileUtils.FileCopyResult result = puller.getSegmentFiles(
        new CloudObjectLocation(
            bucket,
            object0.getKey()
        ), tmpDir
    );
    EasyMock.verify(s3Client);

    Assert.assertEquals(value.length, result.size());
    File expected = new File(tmpDir, "renames-0");
    Assert.assertTrue(expected.exists());
    Assert.assertEquals(value.length, expected.length());
  }

  @Test
  public void testS3ObjectStream() throws IOException
  {
    final String bucket = "bucket";
    final String keyPrefix = "prefix/dir/0";
    final ServerSideEncryptingAmazonS3 s3Client = EasyMock.createStrictMock(ServerSideEncryptingAmazonS3.class);
    final byte[] value = bucket.getBytes(StandardCharsets.UTF_8);

    final File tmpFile = temporaryFolder.newFile("testObjectFile");

    try (OutputStream outputStream = new FileOutputStream(tmpFile)) {
      outputStream.write(value);
    }

    final S3Object object0 = new S3Object();
    object0.setBucketName(bucket);
    object0.setKey(keyPrefix + "/test-object");
    object0.getObjectMetadata().setLastModified(new Date(0));
    object0.setObjectContent(new FileInputStream(tmpFile));

    EasyMock.expect(s3Client.getObject(EasyMock.eq(object0.getBucketName()), EasyMock.eq(object0.getKey())))
            .andReturn(object0)
            .once();
    S3DataSegmentPuller puller = new S3DataSegmentPuller(s3Client);
    EasyMock.replay(s3Client);
    InputStream stream = puller.buildFileObject(URI.create(StringUtils.format("s3://%s/%s", bucket, object0.getKey())))
                               .openInputStream();
    EasyMock.verify(s3Client);
    Assert.assertEquals(bucket, IOUtils.toString(stream, StandardCharsets.UTF_8));
  }

  @Test
  public void testS3ObjectModifiedDate() throws IOException
  {
    final String bucket = "bucket";
    final String keyPrefix = "prefix/dir/0";
    final ServerSideEncryptingAmazonS3 s3Client = EasyMock.createStrictMock(ServerSideEncryptingAmazonS3.class);
    final byte[] value = bucket.getBytes(StandardCharsets.UTF_8);

    final File tmpFile = temporaryFolder.newFile("testObjectFile");

    try (OutputStream outputStream = Files.newOutputStream(tmpFile.toPath())) {
      outputStream.write(value);
    }

    final S3Object object0 = new S3Object();
    object0.setBucketName(bucket);
    object0.setKey(keyPrefix + "/test-object");
    object0.getObjectMetadata().setLastModified(new Date(0));
    object0.setObjectContent(Files.newInputStream(tmpFile.toPath()));

    final ObjectMetadata objectMetadata = new ObjectMetadata();
    objectMetadata.setLastModified(new Date(0));

    EasyMock.expect(s3Client.getObjectMetadata(bucket, object0.getKey()))
            .andReturn(objectMetadata)
            .once();

    S3DataSegmentPuller puller = new S3DataSegmentPuller(s3Client);
    EasyMock.replay(s3Client);
    long modifiedDate = puller.buildFileObject(URI.create(StringUtils.format("s3://%s/%s", bucket, object0.getKey())))
                              .getLastModified();
    EasyMock.verify(s3Client);
    Assert.assertEquals(0, modifiedDate);
  }
}
