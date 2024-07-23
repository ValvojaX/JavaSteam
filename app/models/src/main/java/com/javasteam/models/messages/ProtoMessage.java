package com.javasteam.models.messages;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.javasteam.models.AbstractMessage;
import com.javasteam.models.ProtoHeader;
import com.javasteam.models.headers.ProtoMessageHeader;
import com.javasteam.utils.common.ArrayUtils;
import com.javasteam.utils.proto.ProtoUtils;
import com.javasteam.utils.serializer.Serializer;
import java.nio.ByteOrder;
import java.util.Optional;
import lombok.Getter;

/**
 * ProtoMessage is a class that represents a protobuf message that is sent or received from the
 * Steam network. All incoming and outgoing protobuf messages are represented by this class. It
 * contains the message header and the message body.
 *
 * @param <H> The type of the message header
 * @param <T> The type of the message body
 */
@Getter
public class ProtoMessage<H extends ProtoHeader, T extends GeneratedMessage>
    extends AbstractMessage<H, T> {
  private final H header;
  private T body;

  private ProtoMessage(byte[] data) {
    super(data);
    this.header = parseHeader(data);
  }

  // Here in case header is a gc header
  private ProtoMessage(H header, byte[] bodyBytes) {
    super(ArrayUtils.concat(header.serialize(), bodyBytes));
    this.header = header;
  }

  private ProtoMessage(H header, T body) {
    super(ArrayUtils.concat(header.serialize(), body.toByteArray()));
    this.header = header;
    this.body = body;
  }

  public static <H extends ProtoHeader, T extends GeneratedMessage> ProtoMessage<H, T> of(
      H header, T body) {
    return new ProtoMessage<>(header, body);
  }

  public static <H extends ProtoHeader, T extends GeneratedMessage> ProtoMessage<H, T> fromBytes(
      byte[] data) {
    return new ProtoMessage<>(data);
  }

  public static <H extends ProtoHeader, T extends GeneratedMessage> ProtoMessage<H, T> fromBytes(
      H header, byte[] bodyBytes) {
    return new ProtoMessage<>(header, bodyBytes);
  }

  @Override
  public Optional<T> getBody() {
    return Optional.ofNullable(body);
  }

  @Override
  public T getBody(Class<T> clazz) {
    return ProtoUtils.parseFromBytes(getBodyBytes(), clazz);
  }

  @SuppressWarnings("unchecked")
  protected H parseHeader(byte[] data) {
    return (H) ProtoMessageHeader.fromBytes(data);
  }

  @Override
  protected byte[] getBodyBytes() {
    return getBody()
        .map(GeneratedMessage::toByteArray)
        .orElse(
            ArrayUtils.subarray(
                getData(), getHeader().getSize(), getData().length - getHeader().getSize()));
  }

  @Override
  @SuppressWarnings("unchecked")
  public Serializer getSerializer() {
    var result =
        Serializer.builder(ByteOrder.LITTLE_ENDIAN)
            .addByteArrayField(getHeader().getSize(), getHeader()::serialize, getHeader()::load);

    getBody()
        .ifPresentOrElse(
            body ->
                result.addByteArrayField(
                    body.getSerializedSize(),
                    body::toByteArray,
                    (b) -> {
                      try {
                        this.body = (T) body.newBuilderForType().mergeFrom(b).build();
                      } catch (InvalidProtocolBufferException e) {
                        throw new RuntimeException(e);
                      }
                    }),
            () -> result.addByteArrayField(getBodyBytes().length, this::getBodyBytes, (b) -> {}));
    return result.build();
  }
}
