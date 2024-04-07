package com.javasteam.models.steam.structs;

import com.javasteam.models.steam.BaseStruct;
import com.javasteam.utils.serializer.Serializer;
import java.nio.ByteOrder;
import java.util.Arrays;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(value = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ChannelEncryptRequest extends BaseStruct {
  private int protocolVersion;
  private int universe;
  private byte[] challenge;

  public ChannelEncryptRequest() {
    this.protocolVersion = 1;
    this.universe = 0;
    this.challenge = new byte[16];
  }

  @Override
  public Serializer getSerializer() {
    return Serializer.builder(ByteOrder.LITTLE_ENDIAN)
        .addIntegerField(4, this::getProtocolVersion, this::setProtocolVersion)
        .addIntegerField(4, this::getUniverse, this::setUniverse)
        .addByteArrayField(16, this::getChallenge, this::setChallenge)
        .build();
  }

  @Override
  public String toString() {
    return "protocolVersion: %s\n".formatted(protocolVersion)
        + "universe: %s\n".formatted(universe)
        + "challenge: %s\n".formatted(Arrays.toString(challenge));
  }
}
