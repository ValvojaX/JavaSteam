package com.javasteam.models.steam.messages;

import com.google.protobuf.GeneratedMessage;
import com.javasteam.models.steam.BaseMsg;
import com.javasteam.models.steam.ProtoMessageStruct;
import com.javasteam.models.steam.headers.MsgHeaderProto;
import com.javasteam.utils.common.ArrayUtils;
import com.javasteam.utils.serializer.Serializer;
import java.nio.ByteOrder;
import java.util.Optional;

/**
 * ProtoMessage is a class that represents a protobuf message that is sent or received from the
 * Steam network. All incoming and outgoing protobuf messages are represented by this class. It
 * contains the message header and the message body.
 *
 * @param <H> The type of the message header
 * @param <T> The type of the message body
 */
public class ProtoMessage<H extends GeneratedMessage, T extends GeneratedMessage>
    extends BaseMsg<MsgHeaderProto<H>, T> {
  private MsgHeaderProto<H> header;
  private T body;

  private ProtoMessage(int emsgId, byte[] data) {
    super(emsgId, data);
    this.header = getMsgHeader();
    this.body = getMsgBody().orElse(null);
  }

  private ProtoMessage(int emsgId, MsgHeaderProto<H> header) {
    super(emsgId, header.serialize());
    this.header = header;
  }

  private ProtoMessage(int emsgId, MsgHeaderProto<H> header, T body) {
    super(emsgId, ArrayUtils.concat(header.serialize(), body.toByteArray()));
    this.header = header;
    this.body = body;
  }

  public static <H extends GeneratedMessage, T extends GeneratedMessage> ProtoMessage<H, T> of(
      int emsgId, MsgHeaderProto<H> header) {
    return new ProtoMessage<>(emsgId, header);
  }

  public static <H extends GeneratedMessage, T extends GeneratedMessage> ProtoMessage<H, T> of(
      int emsgId, MsgHeaderProto<H> header, T body) {
    return new ProtoMessage<>(emsgId, header, body);
  }

  public static <H extends GeneratedMessage, T extends GeneratedMessage>
      ProtoMessage<H, T> fromBytes(int emsgId, byte[] data) {
    return new ProtoMessage<>(emsgId, data);
  }

  public static <H extends GeneratedMessage, T extends GeneratedMessage>
      ProtoMessage<H, T> fromBytes(int emsgId, byte[] header, byte[] body) {
    return new ProtoMessage<>(emsgId, ArrayUtils.concat(header, body));
  }

  @Override
  public MsgHeaderProto<H> getMsgHeader() {
    return Optional.ofNullable(header).orElse(MsgHeaderProto.fromBytes(getData()));
  }

  @Override
  @SuppressWarnings("unchecked")
  public Optional<T> getMsgBody() {
    if (this.body != null) {
      return Optional.of(this.body);
    }

    return ProtoMessageStruct.resolveProto(getEmsg())
        .map(proto -> (T) proto.getLoader().apply(getBodyBytes()));
  }

  @Override
  public Serializer getSerializer() {
    var result =
        Serializer.builder(ByteOrder.LITTLE_ENDIAN)
            .addByteArrayField(
                getMsgHeader().getSize(), this::getHeaderBytes, this::setHeaderBytes);
    getMsgBody()
        .ifPresent(
            body ->
                result.addByteArrayField(
                    body.getSerializedSize(), this::getBodyBytes, this::setBodyBytes));
    return result.build();
  }
}
