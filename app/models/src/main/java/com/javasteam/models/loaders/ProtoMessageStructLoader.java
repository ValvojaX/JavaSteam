package com.javasteam.models.loaders;

import static com.javasteam.protobufs.EnumsClientserver.EMsg;
import static com.javasteam.protobufs.SteammessagesBase.CMsgMulti;
import static com.javasteam.protobufs.SteammessagesClientserver.CMsgClientGamesPlayed;
import static com.javasteam.protobufs.SteammessagesClientserver2.CMsgClientServiceCall;
import static com.javasteam.protobufs.SteammessagesClientserver2.CMsgClientServiceCallResponse;
import static com.javasteam.protobufs.SteammessagesClientserver2.CMsgClientServiceMethodLegacyResponse;
import static com.javasteam.protobufs.SteammessagesClientserver2.CMsgGCClient;
import static com.javasteam.protobufs.SteammessagesClientserverFriends.CMsgClientChangeStatus;
import static com.javasteam.protobufs.SteammessagesClientserverFriends.CMsgClientPersonaState;
import static com.javasteam.protobufs.SteammessagesClientserverLogin.CMsgClientLogon;
import static com.javasteam.protobufs.SteammessagesClientserverLogin.CMsgClientLogonResponse;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.javasteam.models.StructLoader;
import com.javasteam.models.ThrowingFunction;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.Getter;

/**
 * ProtoMessageStruct is an enum that maps the EMsg value to the corresponding protobuf message
 * class. It is used to resolve the protobuf message class based on the EMsg value.
 */
@Getter
public enum ProtoMessageStructLoader implements StructLoader<GeneratedMessage> {
  CLIENT_LOGON(EMsg.k_EMsgClientLogon_VALUE, CMsgClientLogon::parseFrom),
  CLIENT_LOGON_RESPONSE(EMsg.k_EMsgClientLogOnResponse_VALUE, CMsgClientLogonResponse::parseFrom),
  MULTI(EMsg.k_EMsgMulti_VALUE, CMsgMulti::parseFrom),
  CLIENT_TO_GC(EMsg.k_EMsgClientToGC_VALUE, CMsgGCClient::parseFrom),
  CLIENT_FROM_GC(EMsg.k_EMsgClientFromGC_VALUE, CMsgGCClient::parseFrom),
  CLIENT_GAMES_PLAYED(EMsg.k_EMsgClientGamesPlayed_VALUE, CMsgClientGamesPlayed::parseFrom),
  CLIENT_CHANGE_STATUS(EMsg.k_EMsgClientChangeStatus_VALUE, CMsgClientChangeStatus::parseFrom),
  PERSONA_STATE(EMsg.k_EMsgClientPersonaState_VALUE, CMsgClientPersonaState::parseFrom),
  CLIENT_SERVICE_CALL(EMsg.k_EMsgClientServiceCall_VALUE, CMsgClientServiceCall::parseFrom),
  CLIENT_SERVICE_CALL_RESPONSE(
      EMsg.k_EMsgClientServiceCallResponse_VALUE, CMsgClientServiceCallResponse::parseFrom),
  CLIENT_SERVICE_METHOD_LEGACY_RESPONSE(
      EMsg.k_EMsgClientServiceMethodLegacyResponse_VALUE,
      CMsgClientServiceMethodLegacyResponse::parseFrom);

  private final int emsg;
  private final Function<byte[], GeneratedMessage> loader;

  <T extends GeneratedMessage> ProtoMessageStructLoader(
      int emsg, ThrowingFunction<byte[], T, InvalidProtocolBufferException> parseFrom) {
    this.emsg = emsg;
    this.loader =
        bytes -> {
          try {
            return parseFrom.apply(bytes);
          } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
          }
        };
  }

  public static Optional<ProtoMessageStructLoader> resolveProto(int emsg) {
    return Stream.of(ProtoMessageStructLoader.values())
        .filter(provider -> provider.emsg == emsg)
        .findFirst();
  }
}
