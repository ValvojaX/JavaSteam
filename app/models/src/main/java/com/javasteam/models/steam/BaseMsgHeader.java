package com.javasteam.models.steam;

import com.javasteam.utils.serializer.Serializer;

/**
 * BaseMsgHeader is a class that represents the header of a message that is sent or received from
 * the Steam network. All incoming and outgoing message headers are represented by this class.
 */
public abstract class BaseMsgHeader implements SerializerProvider {
  public abstract Serializer getSerializer();
}
