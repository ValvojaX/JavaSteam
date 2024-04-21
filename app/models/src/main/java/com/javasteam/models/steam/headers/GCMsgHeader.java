package com.javasteam.models.steam.headers;

import com.javasteam.models.steam.BaseMsgHeader;
import com.javasteam.utils.serializer.Serializer;
import java.nio.ByteOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * GCMsgHeader is a class that represents the header of a message received by the game coordinator.
 */
@Getter
@Setter(value = AccessLevel.PROTECTED)
@AllArgsConstructor
public class GCMsgHeader extends BaseMsgHeader {
  public static final int size = 18;
  private short headerVersion;
  private long targetJobId;
  private long sourceJobId;

  public GCMsgHeader() {
    super();
    this.headerVersion = 1;
    this.targetJobId = -1;
    this.sourceJobId = -1;
  }

  public static GCMsgHeader of(byte[] data) {
    GCMsgHeader gcMsgHeader = new GCMsgHeader();
    gcMsgHeader.load(data);
    return gcMsgHeader;
  }

  @Override
  public Serializer getSerializer() {
    return Serializer.builder(ByteOrder.LITTLE_ENDIAN)
        .addShortField(2, this::getHeaderVersion, this::setHeaderVersion)
        .addLongField(8, this::getTargetJobId, this::setTargetJobId)
        .addLongField(8, this::getSourceJobId, this::setSourceJobId)
        .build();
  }

  @Override
  public String toString() {
    return "headerVersion: %s\n".formatted(headerVersion)
        + "targetJobId: %s\n".formatted(targetJobId)
        + "sourceJobId: %s\n".formatted(sourceJobId);
  }
}
