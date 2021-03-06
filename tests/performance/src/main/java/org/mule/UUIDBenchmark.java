/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.util.UUID;

import org.openjdk.jmh.annotations.Benchmark;

public class UUIDBenchmark extends AbstractBenchmark {

  @Benchmark
  public String UUID() throws MuleException {
    return UUID.getUUID();
  }

}
