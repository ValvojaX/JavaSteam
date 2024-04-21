package com.javasteam.models.steam;

import static com.javasteam.protobufs.EnumsClientserver.EMsg;
import static com.javasteam.protobufs.SteammessagesBase.CMsgMulti;
import static com.javasteam.protobufs.SteammessagesClientserver.CMsgClientGamesPlayed;
import static com.javasteam.protobufs.SteammessagesClientserver2.CMsgGCClient;
import static com.javasteam.protobufs.SteammessagesClientserverFriends.CMsgClientChangeStatus;
import static com.javasteam.protobufs.SteammessagesClientserverFriends.CMsgClientPersonaState;
import static com.javasteam.protobufs.SteammessagesClientserverLogin.CMsgClientLogon;
import static com.javasteam.protobufs.SteammessagesClientserverLogin.CMsgClientLogonResponse;

import com.google.protobuf.GeneratedMessage;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.Getter;

/**
 * ProtoMessageStruct is an enum that maps the EMsg value to the corresponding protobuf message
 * class. It is used to resolve the protobuf message class based on the EMsg value. If a protobuf
 * message needs to be parsed, it needs to be resolved using this enum.
 */
@Getter
public enum ProtoMessageStruct implements StructLoader<GeneratedMessage> {
  EMSG_CLIENT_LOGON(
      EMsg.k_EMsgClientLogon_VALUE, CMsgClientLogon.class, CMsgClientLogon::getDefaultInstance),
  EMSG_CLIENT_LOGON_RESPONSE(
      EMsg.k_EMsgClientLogOnResponse_VALUE,
      CMsgClientLogonResponse.class,
      CMsgClientLogonResponse::getDefaultInstance),
  MULTI(EMsg.k_EMsgMulti_VALUE, CMsgMulti.class, CMsgMulti::getDefaultInstance),
  EMSG_CLIENT_TO_GC(
      EMsg.k_EMsgClientToGC_VALUE, CMsgGCClient.class, CMsgGCClient::getDefaultInstance),
  EMSG_CLIENT_FROM_GC(
      EMsg.k_EMsgClientFromGC_VALUE, CMsgGCClient.class, CMsgGCClient::getDefaultInstance),
  EMSG_CLIENT_GAMES_PLAYED(
      EMsg.k_EMsgClientGamesPlayed_VALUE,
      CMsgClientGamesPlayed.class,
      CMsgClientGamesPlayed::getDefaultInstance),
  EMSG_CLIENT_CHANGE_STATUS(
      EMsg.k_EMsgClientChangeStatus_VALUE,
      CMsgClientChangeStatus.class,
      CMsgClientChangeStatus::getDefaultInstance),
  EMSG_PERSONA_STATE(
      EMsg.k_EMsgClientPersonaState_VALUE,
      CMsgClientPersonaState.class,
      CMsgClientPersonaState::getDefaultInstance);

  private final int emsg;
  private final Class<? extends GeneratedMessage> clazz;
  private final Supplier<? extends GeneratedMessage> getDefaultInstance;
  private final Function<byte[], GeneratedMessage> loader;

  <T extends GeneratedMessage> ProtoMessageStruct(
      int emsg, Class<T> clazz, Supplier<T> getDefaultInstance) {
    this.emsg = emsg;
    this.clazz = clazz;
    this.getDefaultInstance = getDefaultInstance;
    this.loader = this::loadProto;
  }

  public static Optional<ProtoMessageStruct> resolveProto(int emsg) {
    return Stream.of(ProtoMessageStruct.values())
        .filter(provider -> provider.emsg == emsg)
        .findFirst();
  }

  @SuppressWarnings("unchecked")
  private <T extends GeneratedMessage> T loadProto(byte[] data) {
    try {
      return (T) getDefaultInstance.get().newBuilderForType().mergeFrom(data).build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
