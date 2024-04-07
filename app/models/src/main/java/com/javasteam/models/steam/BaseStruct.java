package com.javasteam.models.steam;

import com.javasteam.utils.serializer.Serializer;

/**
 * BaseStruct is a class that represents a struct that is sent or received from the Steam network.
 * Messages that are not protobufs are represented by this class. It contains the struct data.
 */
public abstract class BaseStruct implements SerializerProvider {
  public abstract Serializer getSerializer();

  public abstract String toString();

  public static <T extends BaseStruct> T of(byte[] data, Class<T> clazz) {
    try {
      T struct = clazz.getDeclaredConstructor().newInstance();
      struct.getSerializer().unpack(data);
      return struct;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
