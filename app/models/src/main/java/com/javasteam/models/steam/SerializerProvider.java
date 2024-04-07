package com.javasteam.models.steam;

import com.javasteam.utils.serializer.Serializer;

/**
 * SerializerProvider is an interface that provides a serializer for a class. It is used to
 * serialize and deserialize the class. Provides a default implementation for serialization and
 * deserialization.
 */
public interface SerializerProvider {
  Serializer getSerializer();

  default byte[] serialize() {
    return getSerializer().pack();
  }

  default void load(byte[] data) {
    getSerializer().unpack(data);
  }

  default int getSize() {
    return getSerializer().getSize();
  }
}
