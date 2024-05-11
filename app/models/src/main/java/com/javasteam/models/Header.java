package com.javasteam.models;

import com.javasteam.utils.serializer.Serializer;

/** Marks a class as a message header for a {@link AbstractMessage}. */
public interface Header extends SerializerProvider {
  Serializer getSerializer();

  int getEmsgId();
}
