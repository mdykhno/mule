/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.processor.simple;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.util.AttributeEvaluator;

/**
 * Modifies the payload of a {@link Message} according to the provided value.
 */
public class SetPayloadMessageProcessor extends SimpleMessageProcessor {

  private DataType dataType = DataType.OBJECT;
  private AttributeEvaluator valueEvaluator = new AttributeEvaluator(null);


  @Override
  public Event process(Event event) throws MuleException {
    final Message.Builder builder = Message.builder(event.getMessage());

    final TypedValue typedValue = resolveTypedValue(event);
    builder.payload(typedValue.getValue()).mediaType(typedValue.getDataType().getMediaType());

    return Event.builder(event).message(builder.build()).build();
  }

  private TypedValue resolveTypedValue(Event event) {
    if (valueEvaluator.getRawValue() == null) {
      return new TypedValue(null, dataType);
    } else {
      return valueEvaluator.resolveTypedValue(event, dataType);
    }
  }

  public void setMimeType(String mimeType) {
    setDataType(DataType.builder(dataType).mediaType(mimeType).build());
  }

  public void setEncoding(String encoding) {
    setDataType(DataType.builder(dataType).charset(encoding).build());
  }

  public void setDataType(DataType dataType) {
    if (dataType.getMediaType().getCharset().isPresent()) {
      this.dataType = dataType;
    } else {
      this.dataType = DataType.builder(dataType).build();
    }
  }

  public void setValue(String value) {
    valueEvaluator = new AttributeEvaluator(value);
  }

  @Override
  public void initialise() throws InitialisationException {
    valueEvaluator.initialize(muleContext.getExpressionManager());
  }
}
