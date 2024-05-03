package com.javasteam.models.messages;

import com.google.protobuf.GeneratedMessage;
import com.javasteam.models.AbstractMessage;
import com.javasteam.models.ProtoHeader;
import com.javasteam.models.containers.StructContainer;
import com.javasteam.models.headers.ProtoMessageHeader;
import com.javasteam.utils.common.ArrayUtils;
import com.javasteam.utils.proto.ProtoUtils;
import com.javasteam.utils.serializer.Serializer;
import java.nio.ByteOrder;
import java.util.Optional;

/**
 * ProtoMessage is a class that represents a protobuf message that is sent or received from the
 * Steam network. All incoming and outgoing protobuf messages are represented by this class. It
 * contains the message header and the message body.
 *
 * @param <T> The type of the message body
 */
public class ProtoMessage<T extends GeneratedMessage> extends AbstractMessage<ProtoHeader, T> {
  private ProtoHeader header;
  private T body;
  private byte[] bodyBytes; // Only to be used when message body is unknown

  private ProtoMessage(int emsgId, ProtoHeader header) {
    super(emsgId);
    this.header = header;
  }

  private ProtoMessage(int emsgId, ProtoHeader header, byte[] bodyBytes) {
    super(emsgId);
    this.header = header;
    this.bodyBytes = bodyBytes;
  }

  private ProtoMessage(int emsgId, ProtoHeader header, T body) {
    super(emsgId);
    this.header = header;
    this.body = body;
  }

  public static <T extends GeneratedMessage> ProtoMessage<T> of(int emsgId, ProtoHeader header) {
    return new ProtoMessage<>(emsgId, header);
  }

  public static <T extends GeneratedMessage> ProtoMessage<T> of(
      int emsgId, ProtoHeader header, T body) {
    return new ProtoMessage<>(emsgId, header, body);
  }

  @SuppressWarnings("unchecked")
  public static <T extends GeneratedMessage> ProtoMessage<T> fromBytes(
      int emsgId, ProtoHeader header, byte[] bodyBytes) {
    var body =
        StructContainer.getStructLoader(ProtoUtils.clearProtoMask(emsgId))
            .map(struct -> struct.getLoader().apply(bodyBytes));
    return body.map(b -> new ProtoMessage<>(emsgId, header, (T) b))
        .orElseGet(() -> new ProtoMessage<>(emsgId, header, bodyBytes));
  }

  @SuppressWarnings("unchecked")
  public static <T extends GeneratedMessage> ProtoMessage<T> fromBytes(int emsgId, byte[] data) {
    var header = ProtoMessageHeader.fromBytes(data);
    var bodyBytes = ArrayUtils.subarray(data, header.getSize(), data.length - header.getSize());
    var body =
        StructContainer.getStructLoader(ProtoUtils.clearProtoMask(emsgId))
            .map(struct -> struct.getLoader().apply(bodyBytes));
    return body.map(b -> new ProtoMessage<>(emsgId, header, (T) b))
        .orElseGet(() -> new ProtoMessage<>(emsgId, header, bodyBytes));
  }

  @Override
  public ProtoHeader getMsgHeader() {
    return header;
  }

  @Override
  public Optional<T> getMsgBody() {
    return Optional.ofNullable(this.body);
  }

  @Override
  protected byte[] getMsgBodyBytes() {
    return this.bodyBytes;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Serializer getSerializer() {
    var result =
        Serializer.builder(ByteOrder.LITTLE_ENDIAN)
            .addByteArrayField(
                getMsgHeader().getSize(),
                () -> this.header.serialize(),
                bytes -> this.header = ProtoMessageHeader.fromBytes(bytes));
    getMsgBody()
        .ifPresent(
            body ->
                result.addByteArrayField(
                    body.getSerializedSize(),
                    body::toByteArray,
                    bytes ->
                        this.body =
                            StructContainer.getStructLoader(getEmsgId())
                                .map(struct -> (T) struct.getLoader().apply(bytes))
                                .orElse(null)));
    return result.build();
  }
}
