package com.javasteam.models.loaders;

import static com.javasteam.protobufs.EnumsClientserver.EMsg;
import static com.javasteam.protobufs.SteammessagesBase.CMsgMulti;
import static com.javasteam.protobufs.SteammessagesClientserver.CMsgClientGamesPlayed;
import static com.javasteam.protobufs.SteammessagesClientserver2.CMsgGCClient;
import static com.javasteam.protobufs.SteammessagesClientserverFriends.CMsgClientChangeStatus;
import static com.javasteam.protobufs.SteammessagesClientserverFriends.CMsgClientPersonaState;
import static com.javasteam.protobufs.SteammessagesClientserverLogin.CMsgClientLogon;
import static com.javasteam.protobufs.SteammessagesClientserverLogin.CMsgClientLogonResponse;

import com.google.protobuf.GeneratedMessage;
import com.javasteam.models.StructLoader;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.Getter;

/**
 * ProtoMessageStruct is an enum that maps the EMsg value to the corresponding protobuf message
 * class. It is used to resolve the protobuf message class based on the EMsg value.
 */
@Getter
public enum ProtoMessageStructLoader implements StructLoader<GeneratedMessage> {
  CLIENT_LOGON(EMsg.k_EMsgClientLogon_VALUE, CMsgClientLogon::getDefaultInstance),
  CLIENT_LOGON_RESPONSE(
      EMsg.k_EMsgClientLogOnResponse_VALUE, CMsgClientLogonResponse::getDefaultInstance),
  MULTI(EMsg.k_EMsgMulti_VALUE, CMsgMulti::getDefaultInstance),
  CLIENT_TO_GC(EMsg.k_EMsgClientToGC_VALUE, CMsgGCClient::getDefaultInstance),
  CLIENT_FROM_GC(EMsg.k_EMsgClientFromGC_VALUE, CMsgGCClient::getDefaultInstance),
  CLIENT_GAMES_PLAYED(
      EMsg.k_EMsgClientGamesPlayed_VALUE, CMsgClientGamesPlayed::getDefaultInstance),
  CLIENT_CHANGE_STATUS(
      EMsg.k_EMsgClientChangeStatus_VALUE, CMsgClientChangeStatus::getDefaultInstance),
  PERSONA_STATE(EMsg.k_EMsgClientPersonaState_VALUE, CMsgClientPersonaState::getDefaultInstance);

  private final int emsg;
  private final Supplier<? extends GeneratedMessage> getDefaultInstance;
  private final Function<byte[], GeneratedMessage> loader;

  <T extends GeneratedMessage> ProtoMessageStructLoader(int emsg, Supplier<T> getDefaultInstance) {
    this.emsg = emsg;
    this.getDefaultInstance = getDefaultInstance;
    this.loader = this::loadProto;
  }

  public static Optional<ProtoMessageStructLoader> resolveProto(int emsg) {
    return Stream.of(ProtoMessageStructLoader.values())
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
