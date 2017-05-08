/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util;

import static java.util.regex.Pattern.DOTALL;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;

import java.util.regex.Pattern;

/**
 * This class acts as a wrapper for configuration attributes that support simple text, expression or regular expressions. It can
 * be extended to support other cases too.
 */
public class AttributeEvaluator {

  private static final Pattern SINGLE_EXPRESSION_REGEX_PATTERN = Pattern.compile("^#\\[(?:(?!#\\[).)*\\]$", DOTALL);

  private enum AttributeType {
    EXPRESSION, PARSE_EXPRESSION, STATIC_VALUE
  }

  private final String attributeValue;
  private ExtendedExpressionManager expressionManager;
  private AttributeType attributeType;

  public AttributeEvaluator(String attributeValue) {
    this.attributeValue = sanitize(attributeValue);
  }

  public AttributeEvaluator initialize(final ExtendedExpressionManager expressionManager) {
    this.expressionManager = expressionManager;
    resolveAttributeType();
    return this;
  }

  private String sanitize(String attributeValue) {
    if (attributeValue != null) {
      attributeValue = attributeValue.trim().replaceAll("\r", "").replaceAll("\t", "");
    }

    return attributeValue;
  }

  private void resolveAttributeType() {
    if (attributeValue != null && SINGLE_EXPRESSION_REGEX_PATTERN.matcher(attributeValue).matches()) {
      this.attributeType = AttributeType.EXPRESSION;
    } else if (attributeValue != null && isParseExpression(attributeValue)) {
      this.attributeType = AttributeType.PARSE_EXPRESSION;
    } else {
      this.attributeType = AttributeType.STATIC_VALUE;
    }
  }

  private boolean isParseExpression(String attributeValue) {
    final int beginExpression = attributeValue.indexOf("#[");
    if (beginExpression == -1) {
      return false;
    }
    String remainingString = attributeValue.substring(beginExpression + "#[".length());
    return remainingString.contains("]");
  }

  public boolean isExpression() {
    return this.attributeType.equals(AttributeType.EXPRESSION);
  }

  public boolean isParseExpression() {
    return attributeType.equals(AttributeType.PARSE_EXPRESSION);
  }

  public TypedValue resolveTypedValue(Event event, DataType expectedDataType) {
    if (isExpression()) {
      return expressionManager.evaluate(attributeValue, expectedDataType, event);
    } else if (isParseExpression()) {
      final String value = expressionManager.parse(attributeValue, event, null);
      return new TypedValue(value, DataType.builder().type(String.class).build());
    } else {
      Class<?> type = attributeValue == null ? Object.class : String.class;
      return new TypedValue(attributeValue, DataType.builder(expectedDataType).type(type).build());
    }
  }

  public Object resolveValue(Event event) {
    if (isExpression()) {
      return expressionManager.evaluate(attributeValue, event).getValue();
    } else if (isParseExpression()) {
      return expressionManager.parse(attributeValue, event, null);
    } else {
      return attributeValue;
    }
  }

  public Integer resolveIntegerValue(Event event) {
    final Object value = resolveTypedValue(event, DataType.NUMBER).getValue();
    return value == null ? null : ((Number) value).intValue();
  }

  public String resolveStringValue(Event event) {
    final TypedValue value = resolveTypedValue(event, DataType.STRING);
    return value == null ? null : (String) value.getValue();
  }

  public Boolean resolveBooleanValue(Event event) {
    final TypedValue value = resolveTypedValue(event, DataType.BOOLEAN);
    return value == null ? null : (Boolean) value.getValue();
  }

  public String getRawValue() {
    return attributeValue;
  }
}
