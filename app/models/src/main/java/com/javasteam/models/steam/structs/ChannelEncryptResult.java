package com.javasteam.models.steam.structs;

import com.javasteam.models.steam.BaseStruct;
import com.javasteam.utils.serializer.Serializer;
import java.nio.ByteOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(value = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ChannelEncryptResult extends BaseStruct {
  private int result;

  public ChannelEncryptResult() {
    this.result = 0;
  }

  @Override
  public Serializer getSerializer() {
    return Serializer.builder(ByteOrder.LITTLE_ENDIAN)
        .addIntegerField(4, this::getResult, this::setResult)
        .build();
  }

  @Override
  public String toString() {
    return "result: %s\n".formatted(result);
  }
}
