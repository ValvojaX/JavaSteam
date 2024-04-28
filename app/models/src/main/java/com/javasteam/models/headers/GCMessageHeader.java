package com.javasteam.models.headers;

import com.javasteam.models.ProtoHeader;
import com.javasteam.utils.serializer.Serializer;
import java.nio.ByteOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a header in the Steam protocol used by the Game Coordinator (GC). The header contains
 * information about the message, such as the message type and the size of the message.
 */
@Getter
@Setter(value = AccessLevel.PROTECTED)
@AllArgsConstructor
public class GCMessageHeader implements ProtoHeader {
  public static final int size = 18;
  private short headerVersion;
  private long targetJobId;
  private long sourceJobId;

  public GCMessageHeader() {
    super();
    this.headerVersion = 1;
    this.targetJobId = -1;
    this.sourceJobId = -1;
  }

  public static GCMessageHeader fromBytes(byte[] data) {
    GCMessageHeader gcMessageHeader = new GCMessageHeader();
    gcMessageHeader.load(data);
    return gcMessageHeader;
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
