package com.javasteam.utils.serializer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility class for serializing and deserializing objects. Used for defining how to serialize and
 * deserialize objects from bytes.
 */
public class Serializer {
  private final List<SerializationField<Object>> fields;
  private final ByteOrder byteOrder;

  private Serializer(ByteOrder byteOrder) {
    this.fields = new ArrayList<>();
    this.byteOrder = byteOrder;
  }

  public static SerializerBuilder builder(ByteOrder byteOrder) {
    return new SerializerBuilder(byteOrder);
  }

  public static <T> byte[] pack(
      T value, BiConsumer<ByteBuffer, T> bufferSetter, ByteOrder byteOrder, int size) {
    ByteBuffer buffer = ByteBuffer.allocate(size);
    buffer.order(byteOrder);
    bufferSetter.accept(buffer, value);
    return buffer.array();
  }

  public static <T> T unpack(
      byte[] data, Function<ByteBuffer, T> bufferGetter, ByteOrder byteOrder) {
    return unpack(data, bufferGetter, byteOrder, 0);
  }

  public static <T> T unpack(
      byte[] data, Function<ByteBuffer, T> bufferGetter, ByteOrder byteOrder, int fromIndex) {
    ByteBuffer buffer = ByteBuffer.wrap(data, fromIndex, data.length - fromIndex);
    buffer.order(byteOrder);
    return bufferGetter.apply(buffer);
  }

  public int getSize() {
    return fields.stream().mapToInt(SerializationField::size).sum();
  }

  public byte[] pack(boolean flipArray) {
    int size = fields.stream().mapToInt(SerializationField::size).sum();
    ByteBuffer buffer = ByteBuffer.allocate(size);
    buffer.order(this.byteOrder);
    fields.forEach(field -> field.bufferSetter().accept(buffer, field.fieldGetter().get()));
    if (flipArray) {
      buffer.flip();
    }
    return buffer.array();
  }

  public byte[] pack() {
    return pack(false);
  }

  public void unpack(byte[] data) {
    ByteBuffer buffer = ByteBuffer.wrap(data);
    buffer.order(this.byteOrder);
    fields.forEach(field -> field.fieldSetter().accept(field.bufferGetter().apply(buffer)));
  }

  @SuppressWarnings("unchecked")
  private <T> void addField(SerializationField<T> field) {
    fields.add((SerializationField<Object>) field);
  }

  public static class SerializerBuilder {
    private final Serializer serializer;

    public SerializerBuilder addIntegerField(
        int size, Supplier<Integer> getter, Consumer<Integer> setter) {
      serializer.addField(
          SerializationField.of(size, ByteBuffer::getInt, ByteBuffer::putInt, getter, setter));
      return this;
    }

    public SerializerBuilder addShortField(
        int size, Supplier<Short> getter, Consumer<Short> setter) {
      serializer.addField(
          SerializationField.of(size, ByteBuffer::getShort, ByteBuffer::putShort, getter, setter));
      return this;
    }

    public SerializerBuilder addLongField(int size, Supplier<Long> getter, Consumer<Long> setter) {
      serializer.addField(
          SerializationField.of(size, ByteBuffer::getLong, ByteBuffer::putLong, getter, setter));
      return this;
    }

    public SerializerBuilder addByteField(Supplier<Byte> getter, Consumer<Byte> setter) {
      serializer.addField(
          SerializationField.of(1, ByteBuffer::get, ByteBuffer::put, getter, setter));
      return this;
    }

    public SerializerBuilder addByteArrayField(
        int size, Supplier<byte[]> getter, Consumer<byte[]> setter) {
      serializer.addField(
          SerializationField.of(
              size,
              buffer -> {
                byte[] data = new byte[size];
                buffer.get(data);
                return data;
              },
              ByteBuffer::put,
              getter,
              setter));
      return this;
    }

    public SerializerBuilder addStringField(
        int size, Supplier<String> getter, Consumer<String> setter) {
      serializer.addField(
          SerializationField.of(
              size,
              buffer -> {
                byte[] data = new byte[size];
                buffer.get(data);
                return new String(data);
              },
              (buffer, value) -> buffer.put(value.getBytes()),
              getter,
              setter));
      return this;
    }

    private SerializerBuilder(ByteOrder byteOrder) {
      this.serializer = new Serializer(byteOrder);
    }

    public Serializer build() {
      return serializer;
    }
  }
}
