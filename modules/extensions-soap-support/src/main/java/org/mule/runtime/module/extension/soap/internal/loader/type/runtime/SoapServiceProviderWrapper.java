/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.loader.type.runtime;


import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.extension.api.soap.annotation.CustomTransportProviders;
import org.mule.runtime.module.extension.internal.loader.java.type.ParameterizableTypeElement;
import org.mule.runtime.module.extension.internal.loader.java.type.Type;

import java.util.List;
import java.util.Optional;

/**
 * {@link SoapComponentWrapper} implementation for classes that implements the {@link SoapServiceProvider} interface.
 *
 * @since 4.0
 */
public class SoapServiceProviderWrapper extends SoapComponentWrapper implements ParameterizableTypeElement {

  SoapServiceProviderWrapper(Class<? extends SoapServiceProvider> aClass) {
    super(aClass);
  }

  /**
   * @return a {@link Class} that implements the {@link SoapServiceProvider} interface which this {@link Type} represents.
   */
  @Override
  public Class<? extends SoapServiceProvider> getDeclaringClass() {
    return (Class<? extends SoapServiceProvider>) super.getDeclaringClass();
  }

  public List<SoapCustomTransportProviderTypeWrapper> getCustomTransportProviders() {
    Optional<CustomTransportProviders> customTransport = this.getAnnotation(CustomTransportProviders.class);
    if (customTransport.isPresent()) {
      return stream(customTransport.get().value()).map(SoapCustomTransportProviderTypeWrapper::new).collect(toList());
    }
    return emptyList();
  }
}
