package com.javasteam.models.headers;

import com.javasteam.models.HasJob;
import com.javasteam.models.Header;
import com.javasteam.models.Job;
import com.javasteam.utils.serializer.Serializer;
import java.nio.ByteOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a basic header in the Steam protocol. The header contains information about the
 * message, such as the message type and the size of the message.
 */
@Getter
@Setter(value = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MessageHeader implements Header, HasJob {
  public static final int size = 20;
  private int emsgId;
  private long targetJobId;
  private long sourceJobId;

  public MessageHeader() {
    super();
    this.emsgId = 0;
    this.targetJobId = -1;
    this.sourceJobId = -1;
  }

  public static MessageHeader of(int emsg) {
    return new MessageHeader(emsg, -1, -1);
  }

  public static MessageHeader fromBytes(byte[] data) {
    MessageHeader messageHeader = new MessageHeader();
    messageHeader.load(data);
    return messageHeader;
  }

  @Override
  public void setJob(Job job) {
    this.sourceJobId = job.getSourceJobId();
    this.targetJobId = job.getTargetJobId();
  }

  @Override
  public Serializer getSerializer() {
    return Serializer.builder(ByteOrder.LITTLE_ENDIAN)
        .addIntegerField(4, this::getEmsgId, this::setEmsgId)
        .addLongField(8, this::getTargetJobId, this::setTargetJobId)
        .addLongField(8, this::getSourceJobId, this::setSourceJobId)
        .build();
  }

  @Override
  public String toString() {
    return "emsg: %s\n".formatted(emsgId)
        + "targetJobId: %s\n".formatted(targetJobId)
        + "sourceJobId: %s\n".formatted(sourceJobId);
  }
}
