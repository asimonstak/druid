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

package org.apache.druid.data.input.avro;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.util.ByteBufferInputStream;
import org.apache.druid.guice.annotations.Json;
import org.apache.druid.java.util.common.logger.Logger;
import org.apache.druid.java.util.common.parsers.ParseException;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;

/**
 */
public class InlineSchemasAvroBytesDecoder implements AvroBytesDecoder
{
  private static final Logger LOGGER = new Logger(InlineSchemasAvroBytesDecoder.class);

  private static final byte V1 = 0x1;

  private final Map<Integer, Schema> schemaObjs;
  private final Map<String, Map<String, Object>> schemas;

  @JsonCreator
  public InlineSchemasAvroBytesDecoder(
      @JacksonInject @Json ObjectMapper mapper,
      @JsonProperty("schemas") Map<String, Map<String, Object>> schemas
  ) throws Exception
  {
    Preconditions.checkArgument(
        schemas != null && schemas.size() > 0,
        "at least one schema must be provided in schemas attribute"
    );

    this.schemas = schemas;

    schemaObjs = Maps.newHashMapWithExpectedSize(schemas.size());
    for (Map.Entry<String, Map<String, Object>> e : schemas.entrySet()) {

      int id = Integer.parseInt(e.getKey());

      Map<String, Object> schema = e.getValue();
      String schemaStr = mapper.writeValueAsString(schema);

      LOGGER.debug("Schema string [%s] = [%s]", id, schemaStr);
      schemaObjs.put(id, new Schema.Parser().parse(schemaStr));
    }
  }

  @VisibleForTesting
  public InlineSchemasAvroBytesDecoder(
      Map<Integer, Schema> schemaObjs
  )
  {
    this.schemaObjs = schemaObjs;
    this.schemas = null;
  }

  @JsonProperty
  public Map<String, Map<String, Object>> getSchemas()
  {
    return schemas;
  }

  // It is assumed that record has following format.
  // byte 1 : version, static 0x1
  // byte 2-5 : int schemaId
  // remaining bytes would have avro data
  @Override
  public GenericRecord parse(ByteBuffer bytes)
  {
    if (bytes.remaining() < 5) {
      throw new ParseException(null, "Record must have at least 5 bytes carrying version and schemaId");
    }

    byte version = bytes.get();
    if (version != V1) {
      throw new ParseException(null, "Found record of arbitrary version[%s]", version);
    }

    int schemaId = bytes.getInt();
    Schema schemaObj = schemaObjs.get(schemaId);
    if (schemaObj == null) {
      throw new ParseException(null, "Failed to find schema for id[%s]", schemaId);
    }

    DatumReader<GenericRecord> reader = new GenericDatumReader<>(schemaObj);
    try (ByteBufferInputStream inputStream = new ByteBufferInputStream(Collections.singletonList(bytes))) {
      return reader.read(null, DecoderFactory.get().binaryDecoder(inputStream, null));
    }
    catch (Exception e) {
      throw new ParseException(null, e, "Failed to read Avro message with schema id[%s]", schemaId);
    }
  }
}
