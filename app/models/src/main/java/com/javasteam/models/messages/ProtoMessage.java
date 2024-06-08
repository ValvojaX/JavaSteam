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
 * @param <H> The type of the message header
 * @param <T> The type of the message body
 */
public class ProtoMessage<H extends ProtoHeader, T extends GeneratedMessage>
    extends AbstractMessage<H, T> {
  private H header;
  private T body;
  private byte[] bodyBytes; // Only to be used when message body is unknown

  private ProtoMessage(H header) {
    this.header = header;
  }

  private ProtoMessage(H header, byte[] bodyBytes) {
    this.header = header;
    this.bodyBytes = bodyBytes;
  }

  private ProtoMessage(H header, T body) {
    this.header = header;
    this.body = body;
  }

  public static <H extends ProtoHeader, T extends GeneratedMessage> ProtoMessage<H, T> of(
      H header) {
    return new ProtoMessage<>(header);
  }

  public static <H extends ProtoHeader, T extends GeneratedMessage> ProtoMessage<H, T> of(
      H header, T body) {
    return new ProtoMessage<>(header, body);
  }

  @SuppressWarnings("unchecked")
  public static <H extends ProtoHeader, T extends GeneratedMessage> ProtoMessage<H, T> fromBytes(
      H header, byte[] bodyBytes) {
    var body =
        StructContainer.getStructLoader(ProtoUtils.clearProtoMask(header.getEmsgId()))
            .map(struct -> struct.getLoader().apply(bodyBytes));
    return body.map(b -> new ProtoMessage<>(header, (T) b))
        .orElseGet(() -> new ProtoMessage<>(header, bodyBytes));
  }

  @SuppressWarnings("unchecked")
  public static <H extends ProtoHeader, T extends GeneratedMessage> ProtoMessage<H, T> fromBytes(
      byte[] data) {
    var header = ProtoMessageHeader.fromBytes(data);
    var bodyBytes = ArrayUtils.subarray(data, header.getSize(), data.length - header.getSize());
    var body =
        StructContainer.getStructLoader(ProtoUtils.clearProtoMask(header.getEmsgId()))
            .map(struct -> struct.getLoader().apply(bodyBytes));
    return body.map(b -> new ProtoMessage<>((H) header, (T) b))
        .orElseGet(() -> new ProtoMessage<>((H) header, bodyBytes));
  }

  @Override
  public H getMsgHeader() {
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
                bytes -> header.getSerializer().unpack(bytes));
    getMsgBody()
        .ifPresent(
            body ->
                result.addByteArrayField(
                    body.getSerializedSize(),
                    body::toByteArray,
                    bytes ->
                        this.body =
                            StructContainer.getStructLoader(getEMsg())
                                .map(struct -> (T) struct.getLoader().apply(bytes))
                                .orElse(null)));
    return result.build();
  }
}
