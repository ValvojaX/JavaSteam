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
 * Represents an extended header in the Steam protocol. The header contains information about the
 * message, such as the message type and the size of the message.
 */
@Getter
@Setter(value = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ExtendedMessageHeader implements Header, HasJob {
  public static final int size = 36;
  private int emsgId;
  private byte headerSize;
  private short headerVersion;
  private long targetJobId;
  private long sourceJobId;
  private byte headerCanary;
  private long steamId;
  private int sessionID;

  public ExtendedMessageHeader() {
    super();
    this.emsgId = 0;
    this.headerSize = 0;
    this.headerVersion = 0;
    this.targetJobId = -1;
    this.sourceJobId = -1;
    this.headerCanary = 0;
    this.steamId = -1;
    this.sessionID = -1;
  }

  public static ExtendedMessageHeader of(int emsg) {
    return new ExtendedMessageHeader(emsg, (byte) 0, (short) 0, -1, -1, (byte) 0, -1, -1);
  }

  public static ExtendedMessageHeader fromBytes(byte[] data) {
    ExtendedMessageHeader messageHeader = new ExtendedMessageHeader();
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
        .addByteField(this::getHeaderSize, this::setHeaderSize)
        .addShortField(2, this::getHeaderVersion, this::setHeaderVersion)
        .addLongField(8, this::getTargetJobId, this::setTargetJobId)
        .addLongField(8, this::getSourceJobId, this::setSourceJobId)
        .addByteField(this::getHeaderCanary, this::setHeaderCanary)
        .addLongField(8, this::getSteamId, this::setSteamId)
        .addIntegerField(4, this::getSessionID, this::setSessionID)
        .build();
  }

  @Override
  public String toString() {
    return "emsg: %s\n".formatted(emsgId)
        + "headerSize: %s\n".formatted(headerSize)
        + "headerVersion: %s\n".formatted(headerVersion)
        + "targetJobId: %s\n".formatted(targetJobId)
        + "sourceJobId: %s\n".formatted(sourceJobId)
        + "headerCanary: %s\n".formatted(headerCanary)
        + "steamId: %s\n".formatted(steamId)
        + "sessionID: %s\n".formatted(sessionID);
  }
}
