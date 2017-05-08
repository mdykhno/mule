/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.runtime.connection;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.soap.SoapCustomTransportProvider;
import org.mule.runtime.extension.api.soap.message.MessageDispatcher;
import org.mule.service.http.api.HttpService;
import org.mule.services.soap.api.message.dispatcher.DefaultHttpMessageDispatcher;

class DefaultHttpSoapCustomTransportProvider implements SoapCustomTransportProvider {

  private final HttpService service;

  DefaultHttpSoapCustomTransportProvider(HttpService service) {
    this.service = service;
  }

  @Override
  public MessageDispatcher connect() throws ConnectionException {
    return new DefaultHttpMessageDispatcher(service);
  }

  @Override
  public void disconnect(MessageDispatcher connection) {
    connection.dispose();
  }

  @Override
  public ConnectionValidationResult validate(MessageDispatcher connection) {
    return success();
  }
}
