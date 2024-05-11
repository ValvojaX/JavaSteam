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
  public int getEMsgId() {
    return getMsgHeader().getEmsgId();
  }

  public int getEMsg() {
    return ProtoUtils.clearProtoMask(getEMsgId());
  }

  public String getEMsgName() {
    return ProtoUtils.resolveEMsg(getEMsg()).map(Enum::name).orElse("Unknown");
  }

  public abstract H getMsgHeader();

  public abstract Optional<T> getMsgBody();

  public T getMsgBody(Function<byte[], T> loader) {
    return loader.apply(getMsgBodyBytes());
  }

  public T getMsgBody(StructLoader<T> structLoader) {
    return structLoader.getLoader().apply(getMsgBodyBytes());
  }

  public abstract Serializer getSerializer();

  protected abstract byte[] getMsgBodyBytes();

  @Override
  public String toString() {
    return "emsg: %s (%s)\n".formatted(getEMsg(), getEMsgName())
        + "------ Header ------\n"
        + getMsgHeader().toString()
        + "------ Body ------\n"
        + getMsgBody().map(Object::toString).orElse("No body");
  }
}
