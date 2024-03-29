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

package org.apache.druid.cli;

import com.github.rvesse.airline.annotations.Arguments;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.restrictions.Required;
import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import org.apache.druid.guice.DruidProcessingModule;
import org.apache.druid.guice.QueryRunnerFactoryModule;
import org.apache.druid.guice.QueryableModule;
import org.apache.druid.java.util.common.IAE;
import org.apache.druid.java.util.common.logger.Logger;
import org.apache.druid.segment.IndexIO;

import java.io.File;
import java.util.List;

@Command(
    name = "validate-segments",
    description = "Compare two segments for validation"
)
public class ValidateSegments extends GuiceRunnable
{
  private static final Logger log = new Logger(ValidateSegments.class);

  public ValidateSegments()
  {
    super(log);
  }

  @Arguments(description = "Two directories where each directory contains segment files to validate.")
  @Required
  public List<String> directories;

  @Override
  public void run()
  {
    if (directories.size() != 2) {
      throw new IAE("Please provide two segment directories to compare");
    }
    final Injector injector = makeInjector();
    final IndexIO indexIO = injector.getInstance(IndexIO.class);
    try {
      String dir1 = directories.get(0);
      String dir2 = directories.get(1);
      indexIO.validateTwoSegments(new File(dir1), new File(dir2));
      log.info("Segments [%s] and [%s] are identical", dir1, dir2);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected List<? extends Module> getModules()
  {
    return ImmutableList.of(
        // It's unknown if those modules are required in ValidateSegments.
        // Maybe some of those modules could be removed.
        // See https://github.com/apache/druid/pull/4429#discussion_r123603498
        new DruidProcessingModule(),
        new QueryableModule(),
        new QueryRunnerFactoryModule(),
        binder -> {
          binder.bindConstant().annotatedWith(Names.named("serviceName")).to("druid/tool");
          binder.bindConstant().annotatedWith(Names.named("servicePort")).to(9999);
          binder.bindConstant().annotatedWith(Names.named("tlsServicePort")).to(-1);
        }
    );
  }
}
