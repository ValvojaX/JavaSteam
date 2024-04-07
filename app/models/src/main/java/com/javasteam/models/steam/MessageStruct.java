package com.javasteam.models.steam;

import static com.javasteam.protobufs.EnumsClientserver.EMsg;

import com.javasteam.models.steam.structs.ChannelEncryptRequest;
import com.javasteam.models.steam.structs.ChannelEncryptResponse;
import com.javasteam.models.steam.structs.ChannelEncryptResult;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.Getter;

/**
 * MessageStruct is an enum that maps the EMsg value to the corresponding struct class. It is used
 * to resolve the struct class based on the EMsg value. If a message needs to be parsed, it needs to
 * be resolved using this enum.
 */
@Getter
public enum MessageStruct implements StructLoader<BaseStruct> {
  CHANNEL_ENCRYPT_RESULT(EMsg.k_EMsgChannelEncryptRequest_VALUE, ChannelEncryptRequest.class),
  CHANNEL_ENCRYPT_REQUEST(EMsg.k_EMsgChannelEncryptResponse_VALUE, ChannelEncryptResponse.class),
  CHANNEL_ENCRYPT_RESPONSE(EMsg.k_EMsgChannelEncryptResult_VALUE, ChannelEncryptResult.class);

  private final int emsg;
  private final Class<? extends BaseStruct> clazz;
  private final Function<byte[], BaseStruct> loader;

  <T extends BaseStruct> MessageStruct(int emsg, Class<T> clazz) {
    this.emsg = emsg;
    this.clazz = clazz;
    this.loader = this::loadStruct;
  }

  public static Optional<MessageStruct> resolveStruct(int emsg) {
    return Stream.of(MessageStruct.values()).filter(provider -> provider.emsg == emsg).findFirst();
  }

  @SuppressWarnings("unchecked")
  private <T extends BaseStruct> T loadStruct(byte[] data) {
    try {
      T struct = (T) clazz.getDeclaredConstructor().newInstance();
      struct.load(data);
      return struct;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
