package com.javasteam.utils.serializer;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** SerializationField is a class that represents a field in a serialized object. */
public record SerializationField<F>(
    Integer size,
    Function<ByteBuffer, F> bufferGetter,
    BiConsumer<ByteBuffer, F> bufferSetter,
    Supplier<F> fieldGetter,
    Consumer<F> fieldSetter) {
  public SerializationField(
      Integer size,
      Function<ByteBuffer, F> bufferGetter,
      BiConsumer<ByteBuffer, F> bufferSetter,
      Supplier<F> fieldGetter,
      Consumer<F> fieldSetter) {
    this.size = size;
    this.bufferGetter = bufferGetter;
    this.bufferSetter = bufferSetter;
    this.fieldGetter = fieldGetter;
    this.fieldSetter = fieldSetter;
  }

  public static <F> SerializationField<F> of(
      Integer size,
      Function<ByteBuffer, F> bufferGetter,
      BiConsumer<ByteBuffer, F> bufferSetter,
      Supplier<F> fieldGetter,
      Consumer<F> fieldSetter) {
    return new SerializationField<>(size, bufferGetter, bufferSetter, fieldGetter, fieldSetter);
  }
}
