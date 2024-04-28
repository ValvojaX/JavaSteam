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
import com.javasteam.utils.common.ArrayUtils;
import com.javasteam.utils.proto.ProtoUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Game coordinator that connects to a GC server and listens for messages. Handles the client logon
 * process and sends heartbeats to the GC server. The client can be used to send and receive
 * messages from the GC server.
 */
@Slf4j
public class GameCoordinator implements HasListenerGroup {
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
    log.info("Received client from GC:\n{}", msg);

    CMsgGCClient response =
        msg.getMsgBody().orElseThrow(() -> new RuntimeException("Failed to parse client from GC"));

    if (response.getAppid() != appId) {
      throw new RuntimeException(
          "Received message for appid %s, expected %s".formatted(response.getAppid(), appId));
    }

    var header =
        ProtoUtils.isProto(response.getMsgtype())
            ? GCProtoMessageHeader.fromBytes(response.getPayload().toByteArray())
            : GCMessageHeader.fromBytes(response.getPayload().toByteArray());

    byte[] payload = response.getPayload().toByteArray();

    ProtoMessage message =
        ProtoMessage.fromBytes(
            response.getMsgtype(),
            header,
            ArrayUtils.subarray(payload, header.getSize(), payload.length - header.getSize()));

    log.info("Received message from GC for app {}:\n{}", response.getAppid(), message);

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
        ProtoMessage.of(
            EMsg.k_EMsgClientToGC_VALUE,
            ProtoMessageHeader.of(EMsg.k_EMsgClientToGC_VALUE, header),
            proto);

    log.info("Sending message to GC: {}", message);
    steamClient.write(message);
  }

  @Override
  public HasListenerGroup getInstance() {
    return steamClient;
  }
}
