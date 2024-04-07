package com.javasteam.models.steam.headers;

import com.javasteam.models.steam.BaseMsgHeader;
import com.javasteam.utils.serializer.Serializer;
import java.nio.ByteOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * MsgHeader is a class that represents the header of a message that is sent or received from the
 * Steam network. All incoming and outgoing messages that are not protobufs have a header that is
 * represented by this class.
 */
@Getter
@Setter(value = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MsgHeader extends BaseMsgHeader {
  public static final int size = 20;
  private int emsg;
  private long targetJobId;
  private long sourceJobId;

  public MsgHeader() {
    super();
    this.emsg = 0;
    this.targetJobId = -1;
    this.sourceJobId = -1;
  }

  public static MsgHeader of(int emsg) {
    return new MsgHeader(emsg, -1, -1);
  }

  public static MsgHeader of(byte[] data) {
    MsgHeader msgHeader = new MsgHeader();
    msgHeader.load(data);
    return msgHeader;
  }

  @Override
  public Serializer getSerializer() {
    return Serializer.builder(ByteOrder.LITTLE_ENDIAN)
        .addIntegerField(4, this::getEmsg, this::setEmsg)
        .addLongField(8, this::getTargetJobId, this::setTargetJobId)
        .addLongField(8, this::getSourceJobId, this::setSourceJobId)
        .build();
  }

  @Override
  public String toString() {
    return "emsg: %s\n".formatted(emsg)
        + "targetJobId: %s\n".formatted(targetJobId)
        + "sourceJobId: %s\n".formatted(sourceJobId);
  }
}
