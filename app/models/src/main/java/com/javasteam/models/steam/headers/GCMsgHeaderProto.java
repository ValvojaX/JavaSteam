package com.javasteam.models.steam.headers;

import com.google.protobuf.GeneratedMessage;
import com.javasteam.models.steam.BaseMsgHeader;
import com.javasteam.protobufs.GameCoordinatorMessages;
import com.javasteam.utils.proto.ProtoUtils;
import com.javasteam.utils.serializer.Serializer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * GCMsgHeaderProto is a class that represents the header of a protobuf message that is sent by the
 * game coordinator.
 */
@Getter
@Setter(value = AccessLevel.PROTECTED)
@AllArgsConstructor
public class GCMsgHeaderProto<T extends GeneratedMessage> extends BaseMsgHeader {
  public static final int size = 8;
  private int emsg;
  private int protoLength;
  private T proto;

  public GCMsgHeaderProto(int emsg, int protoLength) {
    super();
    this.emsg = emsg;
    this.protoLength = protoLength;
    this.proto = getDefaultProto();
  }

  public static <T extends GeneratedMessage> GCMsgHeaderProto<T> of(int emsg, T proto) {
    return new GCMsgHeaderProto<>(emsg, proto.getSerializedSize(), proto);
  }

  public static <T extends GeneratedMessage> GCMsgHeaderProto<T> of(int emsg, byte[] data) {
    int headerLength = Serializer.unpack(data, ByteBuffer::getInt, ByteOrder.LITTLE_ENDIAN, 4);
    GCMsgHeaderProto<T> gcMsgHeader = new GCMsgHeaderProto<>(emsg, headerLength);
    gcMsgHeader.load(data);
    return gcMsgHeader;
  }

  public static <T extends GeneratedMessage> GCMsgHeaderProto<T> of(byte[] data) {
    return GCMsgHeaderProto.of(0, data);
  }

  public int getEmsgMasked() {
    return ProtoUtils.setProtoMask(emsg);
  }

  public void setEmsgMasked(int emsg) {
    this.emsg = ProtoUtils.clearProtoMask(emsg);
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

  @SuppressWarnings("unchecked")
  public static <T extends GeneratedMessage> T getDefaultProto() {
    return (T) GameCoordinatorMessages.CMsgProtoBufHeader.getDefaultInstance();
  }

  @Override
  public Serializer getSerializer() {
    return Serializer.builder(ByteOrder.LITTLE_ENDIAN)
        .addIntegerField(4, this::getEmsgMasked, this::setEmsgMasked)
        .addIntegerField(4, this::getProtoLength, this::setProtoLength)
        .addByteArrayField(getProtoLength(), this::getProtoBytes, this::setProtoBytes)
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
