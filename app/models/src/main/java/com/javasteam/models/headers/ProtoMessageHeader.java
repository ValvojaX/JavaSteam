package com.javasteam.models.headers;

import static com.javasteam.protobufs.SteammessagesBase.CMsgProtoBufHeader;

import com.google.protobuf.InvalidProtocolBufferException;
import com.javasteam.models.AbstractProtoHeader;
import com.javasteam.models.HasSessionContext;
import com.javasteam.utils.proto.ProtoUtils;
import com.javasteam.utils.serializer.Serializer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * MsgHeaderProto is a class that represents the header of a protobuf message that is sent or
 * received from the Steam network. All incoming and outgoing protobuf messages have a header that
 * is represented by this class.
 */
@Getter
@Setter(value = AccessLevel.PROTECTED)
public class ProtoMessageHeader extends AbstractProtoHeader<CMsgProtoBufHeader>
    implements HasSessionContext {

  private ProtoMessageHeader(int emsg, CMsgProtoBufHeader proto) {
    super(emsg, proto);
  }

  public static ProtoMessageHeader of(int emsg, CMsgProtoBufHeader proto) {
    return new ProtoMessageHeader(emsg, proto);
  }

  public static ProtoMessageHeader fromBytes(byte[] data) {
    int emsgMasked = Serializer.unpack(data, ByteBuffer::getInt, ByteOrder.LITTLE_ENDIAN);
    int emsg = ProtoUtils.clearProtoMask(emsgMasked);
    int protoLength = Serializer.unpack(data, ByteBuffer::getInt, ByteOrder.LITTLE_ENDIAN, 4);

    try {
      var proto = getDefaultProto().toBuilder().mergeFrom(data, size, protoLength).build();
      return new ProtoMessageHeader(emsg, proto);
    } catch (InvalidProtocolBufferException exception) {
      throw new RuntimeException(
          "Failed to set proto bytes, protoLength: %s".formatted(protoLength), exception);
    }
  }

  public static CMsgProtoBufHeader getDefaultProto() {
    return CMsgProtoBufHeader.getDefaultInstance();
  }

  @Override
  public void setSessionId(Integer sessionId) {
    setProto(getProto().toBuilder().setClientSessionid(sessionId).build());
  }

  @Override
  public void setSteamId(long steamId) {
    setProto(getProto().toBuilder().setSteamid(steamId).build());
  }
}
