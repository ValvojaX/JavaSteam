package com.javasteam.models.headers;

import static com.javasteam.protobufs.GameCoordinatorMessages.CMsgProtoBufHeader;

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
 * Represents a header in the Steam protocol used by the Game Coordinator (GC) that contains a
 * protobuf message. The header contains information about the message, such as the message type and
 * the size of the message.
 */
@Getter
@Setter(value = AccessLevel.PROTECTED)
public class GCProtoMessageHeader extends AbstractProtoHeader<CMsgProtoBufHeader>
    implements HasSessionContext {

  public GCProtoMessageHeader(int emsg, CMsgProtoBufHeader proto) {
    super(emsg, proto);
  }

  public static GCProtoMessageHeader of(int emsg, CMsgProtoBufHeader proto) {
    return new GCProtoMessageHeader(emsg, proto);
  }

  public static GCProtoMessageHeader fromBytes(byte[] data) {
    int emsgMasked = Serializer.unpack(data, ByteBuffer::getInt, ByteOrder.LITTLE_ENDIAN);
    int emsg = ProtoUtils.clearProtoMask(emsgMasked);
    int headerLength = Serializer.unpack(data, ByteBuffer::getInt, ByteOrder.LITTLE_ENDIAN, 4);

    try {
      var proto = getDefaultProto().toBuilder().mergeFrom(data, size, headerLength).build();
      return new GCProtoMessageHeader(emsg, proto);
    } catch (InvalidProtocolBufferException exception) {
      throw new RuntimeException(
          "Failed to set proto bytes, headerLength: %s".formatted(headerLength), exception);
    }
  }

  public static CMsgProtoBufHeader getDefaultProto() {
    return CMsgProtoBufHeader.getDefaultInstance();
  }

  @Override
  public void setSessionId(Integer sessionId) {
    setProto(getProto().toBuilder().setClientSessionId(sessionId).build());
  }

  @Override
  public void setSteamId(long steamId) {
    setProto(getProto().toBuilder().setClientSteamId(steamId).build());
  }
}
