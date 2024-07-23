package com.javasteam.models.messages;

import static com.javasteam.protobufs.EnumsClientserver.EMsg;

import com.javasteam.models.AbstractMessage;
import com.javasteam.models.BaseStruct;
import com.javasteam.models.Header;
import com.javasteam.models.SerializerProvider;
import com.javasteam.models.headers.ExtendedMessageHeader;
import com.javasteam.models.headers.MessageHeader;
import com.javasteam.utils.common.ArrayUtils;
import com.javasteam.utils.proto.ProtoUtils;
import com.javasteam.utils.serializer.Serializer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Optional;
import lombok.Getter;

/**
 * Message is a class that represents a message that is sent or received from the Steam network. All
 * incoming and outgoing messages that are not protobufs are represented by this class. It contains
 * the message header and the message body.
 *
 * @param <H> the type of the message header
 * @param <T> the type of the message body
 */
@Getter
public class Message<H extends Header, T extends BaseStruct> extends AbstractMessage<H, T> {
  private static final List<Integer> BASIC_HEADER_MESSAGES =
      List.of(
          EMsg.k_EMsgChannelEncryptRequest_VALUE,
          EMsg.k_EMsgChannelEncryptResponse_VALUE,
          EMsg.k_EMsgChannelEncryptResult_VALUE);
  private final H header;
  private T body;

  private Message(byte[] data) {
    super(data);
    this.header = parseHeader(data);
  }

  private Message(H header, T body) {
    super(ArrayUtils.concat(header.serialize(), body.serialize()));
    this.header = header;
    this.body = body;
  }

  public static <H extends Header, T extends BaseStruct> Message<H, T> of(H header, T body) {
    return new Message<>(header, body);
  }

  public static <H extends Header, T extends BaseStruct> Message<H, T> fromBytes(byte[] data) {
    return new Message<>(data);
  }

  @Override
  public Optional<T> getBody() {
    return Optional.ofNullable(body);
  }

  @Override
  public T getBody(Class<T> clazz) {
    return BaseStruct.of(getBodyBytes(), clazz);
  }

  @SuppressWarnings("unchecked")
  private H parseHeader(byte[] data) {
    var emsgIdMasked =
        Serializer.unpack(data, ByteBuffer::asIntBuffer, ByteOrder.LITTLE_ENDIAN).get();
    var emsgId = ProtoUtils.clearProtoMask(emsgIdMasked);

    if (BASIC_HEADER_MESSAGES.contains(emsgId)) {
      return (H) MessageHeader.fromBytes(data);
    } else {
      return (H) ExtendedMessageHeader.fromBytes(data);
    }
  }

  @Override
  protected byte[] getBodyBytes() {
    return getBody()
        .map(SerializerProvider::serialize)
        .orElse(
            ArrayUtils.subarray(
                getData(), getHeader().getSize(), getData().length - getHeader().getSize()));
  }

  @Override
  public Serializer getSerializer() {
    var result =
        Serializer.builder(ByteOrder.LITTLE_ENDIAN)
            .addByteArrayField(getHeader().getSize(), getHeader()::serialize, getHeader()::load);
    getBody()
        .ifPresentOrElse(
            body -> result.addByteArrayField(body.getSize(), body::serialize, body::load),
            () -> result.addByteArrayField(getBodyBytes().length, this::getBodyBytes, (b) -> {}));
    return result.build();
  }
}
