package com.javasteam.models.steam.headers;

import static com.javasteam.protobufs.SteammessagesBase.CMsgProtoBufHeader;

import com.google.protobuf.GeneratedMessage;
import com.javasteam.models.steam.BaseMsgHeader;
import com.javasteam.utils.proto.ProtoUtils;
import com.javasteam.utils.serializer.Serializer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * MsgHeaderProto is a class that represents the header of a protobuf message that is sent or
 * received from the Steam network. All incoming and outgoing protobuf messages have a header that
 * is represented by this class.
 *
 * @param <T> the type of the protobuf message body
 */
@Getter
@Setter(value = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MsgHeaderProto<T extends GeneratedMessage> extends BaseMsgHeader {
  public static final int size = 8;
  private int emsg;
  private int protoLength;
  private T proto;

  public MsgHeaderProto(int emsg, int protoLength) {
    super();
    this.emsg = 0;
    this.protoLength = protoLength;
    this.proto = getDefaultProto();
  }

  public static <T extends GeneratedMessage> MsgHeaderProto<T> of(int emsg, T proto) {
    return new MsgHeaderProto<>(emsg, proto.getSerializedSize(), proto);
  }

  public static <T extends GeneratedMessage> MsgHeaderProto<T> fromBytes(int emsg, byte[] data) {
    int protoLength = Serializer.unpack(data, ByteBuffer::getInt, ByteOrder.LITTLE_ENDIAN, 4);
    MsgHeaderProto<T> msgHeader = new MsgHeaderProto<>(emsg, protoLength);
    msgHeader.load(data);
    return msgHeader;
  }

  public static <T extends GeneratedMessage> MsgHeaderProto<T> fromBytes(byte[] data) {
    return MsgHeaderProto.fromBytes(0, data);
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
    return (T) CMsgProtoBufHeader.getDefaultInstance();
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
