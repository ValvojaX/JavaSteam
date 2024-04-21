package com.javasteam.models.steam.messages;

import com.javasteam.models.steam.BaseMsg;
import com.javasteam.models.steam.BaseStruct;
import com.javasteam.models.steam.MessageStruct;
import com.javasteam.models.steam.headers.MsgHeader;
import com.javasteam.utils.common.ArrayUtils;
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
public class Message<T extends BaseStruct> extends BaseMsg<MsgHeader, T> {
  private MsgHeader header;
  private T body;

  private Message(int emsgId, byte[] data) {
    super(emsgId, data);
    this.header = getMsgHeader();
    this.body = getMsgBody().orElse(null);
  }

  private Message(int emsgId, MsgHeader header) {
    super(emsgId, header.serialize());
    this.header = header;
  }

  private Message(int emsgId, MsgHeader header, T body) {
    super(emsgId, ArrayUtils.concat(header.serialize(), body.serialize()));
    this.header = header;
    this.body = body;
  }

  public static <T extends BaseStruct> Message<T> of(int emsgId, MsgHeader header) {
    return new Message<>(emsgId, header);
  }

  public static <T extends BaseStruct> Message<T> of(int emsgId, MsgHeader header, T body) {
    return new Message<>(emsgId, header, body);
  }

  public static <T extends BaseStruct> Message<T> fromBytes(int emsgId, byte[] data) {
    return new Message<>(emsgId, data);
  }

  public static <T extends BaseStruct> Message<T> fromBytes(
      int emsgId, byte[] header, byte[] body) {
    return new Message<>(emsgId, ArrayUtils.concat(header, body));
  }

  @Override
  public MsgHeader getMsgHeader() {
    return Optional.ofNullable(header).orElse(MsgHeader.fromBytes(getData()));
  }

  @Override
  @SuppressWarnings("unchecked")
  public Optional<T> getMsgBody() {
    if (body != null) {
      return Optional.of(body);
    }

    return MessageStruct.resolveStruct(getEmsg())
        .map(struct -> (T) struct.getLoader().apply(getBodyBytes()));
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
                result.addByteArrayField(body.getSize(), this::getBodyBytes, this::setBodyBytes));
    return result.build();
  }
}
