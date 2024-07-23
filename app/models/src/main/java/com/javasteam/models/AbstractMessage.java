package com.javasteam.models;

import com.javasteam.utils.proto.ProtoUtils;
import com.javasteam.utils.serializer.Serializer;
import java.util.Optional;
import java.util.function.Function;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents a message in the Steam protocol. A message consists of a header and a body. The header
 * contains information about the message, such as the message type and the size of the message. The
 * body contains the actual data of the message.
 *
 * @param <H> The type of the message header
 * @param <T> The type of the message body
 */
@Slf4j
@Getter
public abstract class AbstractMessage<H extends Header, T> implements SerializerProvider {
  private final byte[] data;

  protected AbstractMessage(byte[] data) {
    this.data = data;
  }

  public int getEMsg() {
    return ProtoUtils.clearProtoMask(getHeader().getEmsgId());
  }

  public String getEMsgName() {
    return ProtoUtils.resolveEMsg(getEMsg()).map(Enum::name).orElse("Unknown");
  }

  public T getBody(Function<byte[], T> loader) {
    return loader.apply(getBodyBytes());
  }

  protected byte[] getHeaderBytes() {
    return getHeader().serialize();
  }

  public abstract Optional<T> getBody();

  public abstract H getHeader();

  public abstract T getBody(Class<T> clazz);

  protected abstract byte[] getBodyBytes();

  public abstract Serializer getSerializer();

  @Override
  public String toString() {
    return "emsg: %s (%s)\n".formatted(getEMsg(), getEMsgName())
        + "------ Header ------\n"
        + getHeader().toString()
        + "------ Body ------\n"
        + getBody().map(Object::toString).orElse("No body");
  }
}
