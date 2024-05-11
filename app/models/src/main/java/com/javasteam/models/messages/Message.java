package com.javasteam.models.messages;

import com.javasteam.models.AbstractMessage;
import com.javasteam.models.BaseStruct;
import com.javasteam.models.Header;
import com.javasteam.models.containers.StructContainer;
import com.javasteam.models.headers.MessageHeader;
import com.javasteam.utils.common.ArrayUtils;
import com.javasteam.utils.proto.ProtoUtils;
import com.javasteam.utils.serializer.Serializer;
import java.nio.ByteOrder;
import java.util.Optional;

/**
 * Message is a class that represents a message that is sent or received from the Steam network. All
 * incoming and outgoing messages that are not protobufs are represented by this class. It contains
 * the message header and the message body.
 *
 * @param <H> the type of the message header
 * @param <T> the type of the message body
 */
public class Message<H extends Header, T extends BaseStruct> extends AbstractMessage<H, T> {
  private H header;
  private T body;
  private byte[] bodyBytes; // Only to be used when message body is unknown

  private Message(H header) {
    this.header = header;
  }

  private Message(H header, byte[] bodyBytes) {
    this.header = header;
    this.bodyBytes = bodyBytes;
  }

  private Message(H header, T body) {
    this.header = header;
    this.body = body;
  }

  public static <H extends Header, T extends BaseStruct> Message<H, T> of(H header) {
    return new Message<>(header);
  }

  public static <H extends Header, T extends BaseStruct> Message<H, T> of(H header, T body) {
    return new Message<>(header, body);
  }

  @SuppressWarnings("unchecked")
  public static <H extends Header, T extends BaseStruct> Message<H, T> fromBytes(byte[] data) {
    var header = MessageHeader.fromBytes(data);

    var bodyBytes = ArrayUtils.subarray(data, header.getSize(), data.length - header.getSize());
    var body =
        StructContainer.getStructLoader(ProtoUtils.clearProtoMask(header.getEmsgId()))
            .map(struct -> struct.getLoader().apply(bodyBytes));
    return body.map(b -> new Message<>((H) header, (T) b))
        .orElseGet(() -> new Message<>((H) header, bodyBytes));
  }

  @Override
  public H getMsgHeader() {
    return header;
  }

  @Override
  public Optional<T> getMsgBody() {
    return Optional.ofNullable(body);
  }

  @Override
  protected byte[] getMsgBodyBytes() {
    return bodyBytes;
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
                    body.getSize(),
                    body::serialize,
                    bytes ->
                        this.body =
                            StructContainer.getStructLoader(getEMsg())
                                .map(struct -> (T) struct.getLoader().apply(bytes))
                                .orElse(null)));
    return result.build();
  }
}
