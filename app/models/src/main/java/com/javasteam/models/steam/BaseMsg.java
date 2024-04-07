package com.javasteam.models.steam;

import com.javasteam.utils.common.ArrayUtils;
import com.javasteam.utils.proto.ProtoUtils;
import com.javasteam.utils.serializer.Serializer;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * BaseMsg is a class that represents a message that is sent or received from the Steam network. All
 * incoming and outgoing messages are represented by this class. It contains the message header and
 * the message body.
 *
 * @param <H> The type of the message header
 * @param <T> The type of the message body
 */
@Slf4j
@Getter
public abstract class BaseMsg<H extends BaseMsgHeader, T> implements SerializerProvider {
  private final int emsgId;
  private final int emsg;
  private final byte[] data;
  private final String emsgName;

  protected BaseMsg(int emsgId, byte[] data) {
    this.emsgId = emsgId;
    this.emsg = ProtoUtils.clearProtoMask(emsgId);
    this.data = data;
    this.emsgName = ProtoUtils.resolveEMsg(emsg).map(Enum::name).orElse("Unknown");
  }

  public abstract H getMsgHeader();

  public abstract Optional<T> getMsgBody();

  public abstract Serializer getSerializer();

  @SuppressWarnings("unchecked")
  public Optional<T> getMsgBody(StructLoader<?> structLoader) {
    try {
      return (Optional<T>) Optional.of(structLoader.getLoader().apply(getBodyBytes()));
    } catch (Exception e) {
      log.error("Error while loading struct", e);
      return Optional.empty();
    }
  }

  public byte[] getHeaderBytes() {
    return ArrayUtils.subarray(data, 0, getMsgHeader().getSize());
  }

  public void setHeaderBytes(byte[] headerBytes) {
    System.arraycopy(headerBytes, 0, data, 0, headerBytes.length);
  }

  public byte[] getBodyBytes() {
    return ArrayUtils.subarray(
        data, getMsgHeader().getSize(), data.length - getMsgHeader().getSize());
  }

  public void setBodyBytes(byte[] bodyBytes) {
    System.arraycopy(bodyBytes, 0, data, getMsgHeader().getSize(), bodyBytes.length);
  }

  @Override
  public String toString() {
    return "emsg: %s (%s)\n".formatted(emsg, emsgName)
        + "------ Header ------\n"
        + getMsgHeader().toString()
        + "------ Body ------\n"
        + getMsgBody().map(Object::toString).orElse("No body");
  }
}
