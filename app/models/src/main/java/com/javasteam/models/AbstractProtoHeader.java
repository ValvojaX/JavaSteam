package com.javasteam.models;

import com.google.protobuf.GeneratedMessage;
import com.javasteam.utils.proto.ProtoUtils;
import com.javasteam.utils.serializer.Serializer;
import java.nio.ByteOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a header in the Steam protocol that contains a protobuf message. The header contains
 * information about the message, such as the message type and the size of the message.
 *
 * @param <T> The type of the protobuf message
 */
@Getter
@Setter(value = AccessLevel.PROTECTED)
@AllArgsConstructor
public abstract class AbstractProtoHeader<T extends GeneratedMessage> implements ProtoHeader {
  public static final int size = 8;
  private int emsg;
  private int protoLength;
  private T proto;

  public AbstractProtoHeader(int emsg, T proto) {
    super();
    this.emsg = emsg;
    this.proto = proto;
    this.protoLength = proto.getSerializedSize();
  }

  public int getEmsgMasked() {
    return ProtoUtils.setProtoMask(emsg);
  }

  public void setEmsgMasked(int emsg) {
    this.emsg = ProtoUtils.clearProtoMask(emsg);
  }

  public int getProtoLength() {
    return proto.getSerializedSize();
  }

  public byte[] getProtoBytes() {
    return proto.toByteArray();
  }

  @SuppressWarnings("unchecked")
  public void setProtoBytes(byte[] protoBytes) {
    try {
      proto = (T) proto.newBuilderForType().mergeFrom(protoBytes).build();
    } catch (Exception e) {
      throw new RuntimeException("Failed to set proto bytes", e);
    }
  }

  @Override
  public Serializer getSerializer() {
    return Serializer.builder(ByteOrder.LITTLE_ENDIAN)
        .addIntegerField(4, this::getEmsgMasked, this::setEmsgMasked)
        .addIntegerField(4, this::getProtoLength, this::setProtoLength)
        .addByteArrayField(proto.getSerializedSize(), this::getProtoBytes, this::setProtoBytes)
        .build();
  }

  @Override
  public String toString() {
    return "emsg: %s\n".formatted(emsg)
        + "protoLength: %s\n".formatted(protoLength)
        + "------ Proto ------\n"
        + proto.toString();
  }
}
