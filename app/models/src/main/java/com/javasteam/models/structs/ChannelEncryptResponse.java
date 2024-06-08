package com.javasteam.models.structs;

import com.javasteam.models.BaseStruct;
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
public class ChannelEncryptResponse extends BaseStruct {
  private int protocolVersion;
  private int keySize;
  private byte[] key;
  private int crc32;
  private int unknown;

  public ChannelEncryptResponse() {
    this.protocolVersion = 1;
    this.keySize = 128;
    this.key = new byte[128];
    this.crc32 = 0;
    this.unknown = 0;
  }

  @Override
  public Serializer getSerializer() {
    return Serializer.builder(ByteOrder.LITTLE_ENDIAN)
        .addIntegerField(4, this::getProtocolVersion, this::setProtocolVersion)
        .addIntegerField(4, this::getKeySize, this::setKeySize)
        .addByteArrayField(128, this::getKey, this::setKey)
        .addIntegerField(4, this::getCrc32, this::setCrc32)
        .addIntegerField(4, this::getUnknown, this::setUnknown)
        .build();
  }

  @Override
  public String toString() {
    return "protocolVersion: %s\n".formatted(protocolVersion)
        + "keySize: %s\n".formatted(keySize)
        + "key: %s\n".formatted(Arrays.toString(key))
        + "crc32: %s\n".formatted(crc32)
        + "unknown: %s\n".formatted(unknown);
  }
}
