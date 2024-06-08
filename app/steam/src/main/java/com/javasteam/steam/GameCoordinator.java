package com.javasteam.steam;

import static com.javasteam.protobufs.EnumsClientserver.EMsg;
import static com.javasteam.protobufs.SteammessagesBase.CMsgProtoBufHeader;
import static com.javasteam.protobufs.SteammessagesClientserver2.CMsgGCClient;

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;
import com.javasteam.models.AbstractMessage;
import com.javasteam.models.headers.GCMessageHeader;
import com.javasteam.models.headers.GCProtoMessageHeader;
import com.javasteam.models.headers.ProtoMessageHeader;
import com.javasteam.models.messages.ProtoMessage;
import com.javasteam.protobufs.GameCoordinatorMessages;
import com.javasteam.steam.handlers.HasMessageHandler;
import com.javasteam.utils.common.ArrayUtils;
import com.javasteam.utils.proto.ProtoUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Game coordinator that connects to a GC server and listens for messages. Handles the client logon
 * process and sends heartbeats to the GC server. The client can be used to send and receive
 * messages from the GC server.
 */
@Slf4j
public class GameCoordinator implements HasMessageHandler {
  private final SteamClient steamClient;
  private final int appId;

  public GameCoordinator(SteamClient steamClient, int appId) {
    this.steamClient = steamClient;
    this.appId = appId;
    addMessageListeners();
  }

  private void addMessageListeners() {
    steamClient.addMessageListener(EMsg.k_EMsgClientFromGC_VALUE, this::onClientFromGC);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void onClientFromGC(AbstractMessage<ProtoMessageHeader, CMsgGCClient> msg) {
    log.debug("Received client from GC:\n{}", msg);

    CMsgGCClient response =
        msg.getMsgBody().orElseThrow(() -> new RuntimeException("Failed to parse client from GC"));

    if (response.getAppid() != appId) {
      log.warn("Received message for appid %s, expected %s".formatted(response.getAppid(), appId));
      return;
    }

    var header =
        ProtoUtils.isProto(response.getMsgtype())
            ? GCProtoMessageHeader.fromBytes(response.getPayload().toByteArray())
            : GCMessageHeader.fromBytes(response.getMsgtype(), response.getPayload().toByteArray());

    byte[] payload = response.getPayload().toByteArray();

    ProtoMessage message =
        ProtoMessage.fromBytes(
            header,
            ArrayUtils.subarray(payload, header.getSize(), payload.length - header.getSize()));

    log.debug("Received message from GC for app {}:\n{}", response.getAppid(), message);

    steamClient.notifyMessageListeners(message);
  }

  public <T extends GeneratedMessage> void write(int emsg, T body) {
    var protoHeader =
        GCProtoMessageHeader.of(
            emsg, GameCoordinatorMessages.CMsgProtoBufHeader.getDefaultInstance());

    var proto =
        CMsgGCClient.newBuilder(CMsgGCClient.getDefaultInstance())
            .setAppid(appId)
            .setMsgtype(ProtoUtils.setProtoMask(emsg))
            .setPayload(
                ByteString.copyFrom(ArrayUtils.concat(protoHeader.serialize(), body.toByteArray())))
            .build();

    var header =
        CMsgProtoBufHeader.newBuilder(CMsgProtoBufHeader.getDefaultInstance())
            .setRoutingAppid(730)
            .build();

    var message =
        ProtoMessage.of(ProtoMessageHeader.of(EMsg.k_EMsgClientToGC_VALUE, header), proto);

    log.debug("Sending message to GC: {}", message);
    steamClient.sendMessage(message);
  }

  @Override
  public HasMessageHandler getInstance() {
    return steamClient;
  }
}
