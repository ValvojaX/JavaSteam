package com.javasteam.models;

import com.javasteam.utils.proto.ProtoUtils;
import com.javasteam.utils.serializer.Serializer;
import java.util.Optional;
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
  private final int emsgId;
  private final int emsg;
  private final String emsgName;

  protected AbstractMessage(int emsgId) {
    this.emsgId = emsgId;
    this.emsg = ProtoUtils.clearProtoMask(emsgId);
    this.emsgName = ProtoUtils.resolveEMsg(emsg).map(Enum::name).orElse("Unknown");
  }

  public abstract H getMsgHeader();

  public abstract Optional<T> getMsgBody();

  public abstract Serializer getSerializer();

  @Override
  public String toString() {
    return "emsg: %s (%s)\n".formatted(emsg, emsgName)
        + "------ Header ------\n"
        + getMsgHeader().toString()
        + "------ Body ------\n"
        + getMsgBody().map(Object::toString).orElse("No body");
  }
}
