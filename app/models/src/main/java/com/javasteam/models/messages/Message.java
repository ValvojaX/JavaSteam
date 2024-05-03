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
 * @param <T> the type of the message body
 */
public class Message<T extends BaseStruct> extends AbstractMessage<Header, T> {
  private Header header;
  private T body;
  private byte[] bodyBytes; // Only to be used when message body is unknown

  private Message(int emsgId, Header header) {
    super(emsgId);
    this.header = header;
  }

  private Message(int emsgId, Header header, byte[] bodyBytes) {
    super(emsgId);
    this.header = header;
    this.bodyBytes = bodyBytes;
  }

  private Message(int emsgId, Header header, T body) {
    super(emsgId);
    this.header = header;
    this.body = body;
  }

  public static <T extends BaseStruct> Message<T> of(int emsgId, Header header) {
    return new Message<>(emsgId, header);
  }

  public static <T extends BaseStruct> Message<T> of(int emsgId, Header header, T body) {
    return new Message<>(emsgId, header, body);
  }

  @SuppressWarnings("unchecked")
  public static <T extends BaseStruct> Message<T> fromBytes(int emsgId, byte[] data) {
    var header = MessageHeader.fromBytes(data);

    var bodyBytes = ArrayUtils.subarray(data, header.getSize(), data.length - header.getSize());
    var body =
        StructContainer.getStructLoader(ProtoUtils.clearProtoMask(emsgId))
            .map(struct -> struct.getLoader().apply(bodyBytes));
    return body.map(b -> new Message<>(emsgId, header, (T) b))
        .orElseGet(() -> new Message<>(emsgId, header, bodyBytes));
  }

  @Override
  public Header getMsgHeader() {
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
                bytes -> this.header = MessageHeader.fromBytes(bytes));
    getMsgBody()
        .ifPresent(
            body ->
                result.addByteArrayField(
                    body.getSize(),
                    body::serialize,
                    bytes ->
                        this.body =
                            StructContainer.getStructLoader(getEmsg())
                                .map(struct -> (T) struct.getLoader().apply(bytes))
                                .orElse(null)));
    return result.build();
  }
}
