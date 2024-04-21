package com.javasteam.steam;

import static com.javasteam.protobufs.EnumsClientserver.EMsg;
import static com.javasteam.protobufs.SteammessagesBase.CMsgProtoBufHeader;
import static com.javasteam.protobufs.SteammessagesClientserver.CMsgClientGamesPlayed;
import static com.javasteam.protobufs.SteammessagesClientserverFriends.CMsgClientChangeStatus;
import static com.javasteam.protobufs.SteammessagesClientserverFriends.CMsgClientPersonaState;
import static com.javasteam.protobufs.SteammessagesClientserverLogin.CMsgClientLogon;
import static com.javasteam.protobufs.SteammessagesClientserverLogin.CMsgClientLogonResponse;

import com.javasteam.models.steam.BaseMsg;
import com.javasteam.models.steam.BaseMsgHeader;
import com.javasteam.models.steam.headers.MsgHeaderProto;
import com.javasteam.models.steam.messages.ProtoMessage;
import com.javasteam.steam.common.EPersonaState;
import com.javasteam.steam.common.EResult;
import com.javasteam.steam.steamid.SteamId;
import com.javasteam.steam.steamid.Type;
import com.javasteam.steam.steamid.Universe;
import com.javasteam.webapi.endpoints.steamdirectory.SteamWebDirectoryRESTAPIClient;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Steam client that connects to a CM server and listens for messages. Handles the client logon
 * process and sends heartbeats to the CM server. The client can be used to send and receive
 * messages from the CM server.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * SteamClient client = new SteamClient(webDirectoryClient);
 * client.connect();
 * client.login(username, password);
 * client.addMessageListener(EMsg.k_EMsgClientLogon, msg -> {
 *     // Handle client logon message
 * });
 * }</pre>
 */
@Slf4j
public class SteamClient extends SteamCMClient {
  @Getter private final SteamSessionContext sessionContext;
  private final ScheduledExecutorService executor;

  public SteamClient(SteamWebDirectoryRESTAPIClient webDirectoryClient) {
    super(webDirectoryClient);
    this.addMessageListeners();
    this.sessionContext = new SteamSessionContext();
    this.executor = Executors.newSingleThreadScheduledExecutor();
  }

  private void addMessageListeners() {
    this.addMessageListener(EMsg.k_EMsgClientLogOnResponse_VALUE, this::onClientLogonResponse);
    this.addMessageListener(EMsg.k_EMsgClientPersonaState_VALUE, this::onClientPersonaState);
  }

  private void onClientPersonaState(
      BaseMsg<MsgHeaderProto<CMsgProtoBufHeader>, CMsgClientPersonaState> msg) {
    log.info("Received client persona state:\n{}", msg);
  }

  public void onClientLogonResponse(
      BaseMsg<MsgHeaderProto<CMsgProtoBufHeader>, CMsgClientLogonResponse> msg) {
    log.info("Received client logon response:\n{}", msg);

    CMsgClientLogonResponse response =
        msg.getMsgBody()
            .orElseThrow(() -> new RuntimeException("Failed to parse client logon response"));

    if (response.getEresult() != EResult.OK) {
      throw new RuntimeException("Failed to log in, EResult: " + response.getEresult());
    }

    this.sessionContext.setSteamId(SteamId.of(response.getClientSuppliedSteamid()));
    this.sessionContext.setSessionId(msg.getMsgHeader().getProto().getClientSessionid());

    log.info("Starting client heartbeat, interval: {} seconds", response.getHeartbeatSeconds());
    int heartbeatInterval = response.getHeartbeatSeconds();
    this.executor.scheduleAtFixedRate(
        this::sendHeartbeat,
        heartbeatInterval,
        heartbeatInterval,
        java.util.concurrent.TimeUnit.SECONDS);
  }

  private void sendHeartbeat() {
    log.debug("Sending client heartbeat");
    SteamId steamId =
        this.sessionContext
            .getSteamIdOptional()
            .orElseThrow(() -> new RuntimeException("Steam ID is not set"));

    CMsgClientLogonResponse response = CMsgClientLogonResponse.newBuilder().build();

    CMsgProtoBufHeader header =
        CMsgProtoBufHeader.newBuilder().setSteamid(steamId.toSteamId64()).build();

    ProtoMessage<CMsgProtoBufHeader, CMsgClientLogonResponse> message =
        ProtoMessage.of(
            EMsg.k_EMsgClientHeartBeat_VALUE,
            MsgHeaderProto.of(EMsg.k_EMsgClientHeartBeat_VALUE, header).serialize(),
            response.toByteArray());
    this.write(message);
  }

  public void loginAnonymous() {
    SteamId steamId = SteamId.of(Universe.PUBLIC, Type.ANON_USER);
    this.sessionContext.setSteamId(steamId);
    log.info("Logging in anonymously with Steam ID: {}", steamId);

    CMsgClientLogon logonMessage =
        CMsgClientLogon.newBuilder()
            .setClientPackageVersion(SteamProtocol.DEFAULT_CLIENT_PACKAGE_VERSION)
            .setProtocolVersion(SteamProtocol.DEFAULT_PROTOCOL_VERSION)
            .build();

    CMsgProtoBufHeader headerProto =
        CMsgProtoBufHeader.newBuilder().setSteamid(steamId.toSteamId64()).build();

    ProtoMessage<CMsgProtoBufHeader, CMsgClientLogon> message =
        ProtoMessage.of(
            EMsg.k_EMsgClientLogon_VALUE,
            MsgHeaderProto.of(EMsg.k_EMsgClientLogon_VALUE, headerProto).serialize(),
            logonMessage.toByteArray());

    log.info("Sending client logon message:\n{}", message);
    this.write(message);
  }

  public void login(String username, String password) {
    SteamId steamId = SteamId.of(Universe.PUBLIC, Type.INDIVIDUAL);
    this.sessionContext.setSteamId(steamId);

    log.info("Logging in with username: {} and Steam ID: {}", username, steamId);

    CMsgClientLogon logonMessage =
        CMsgClientLogon.newBuilder()
            .setClientPackageVersion(SteamProtocol.DEFAULT_CLIENT_PACKAGE_VERSION)
            .setProtocolVersion(SteamProtocol.DEFAULT_PROTOCOL_VERSION)
            .setAccountName(username)
            .setPassword(password)
            .build();

    CMsgProtoBufHeader headerProto =
        CMsgProtoBufHeader.newBuilder().setSteamid(steamId.toSteamId64()).build();

    ProtoMessage<CMsgProtoBufHeader, CMsgClientLogon> message =
        ProtoMessage.of(
            EMsg.k_EMsgClientLogon_VALUE,
            MsgHeaderProto.of(EMsg.k_EMsgClientLogon_VALUE, headerProto).serialize(),
            logonMessage.toByteArray());

    log.info("Sending client logon message:\n{}", message);
    this.write(message);
  }

  public void setState(EPersonaState state) {
    var proto = CMsgClientChangeStatus.newBuilder().setPersonaState(state.getCode()).build();

    var header =
        CMsgProtoBufHeader.newBuilder(CMsgProtoBufHeader.getDefaultInstance())
            .setSteamid(getSessionContext().getSteamId().toSteamId64())
            .setClientSessionid(getSessionContext().getSessionId())
            .build();

    var message =
        ProtoMessage.of(
            EMsg.k_EMsgClientChangeStatus_VALUE,
            MsgHeaderProto.of(EMsg.k_EMsgClientChangeStatus_VALUE, header).serialize(),
            proto.toByteArray());

    log.info("Sending change status message: {}", message);
    write(message);
  }

  public void setGamesPlayed(List<Integer> appIds) {
    var proto =
        CMsgClientGamesPlayed.newBuilder(CMsgClientGamesPlayed.getDefaultInstance())
            .addAllGamesPlayed(
                appIds.stream()
                    .map(
                        appId ->
                            CMsgClientGamesPlayed.GamePlayed.newBuilder().setGameId(appId).build())
                    .toList())
            .build();

    var header =
        CMsgProtoBufHeader.newBuilder(CMsgProtoBufHeader.getDefaultInstance())
            .setSteamid(getSessionContext().getSteamId().toSteamId64())
            .setClientSessionid(getSessionContext().getSessionId())
            .build();

    var message =
        ProtoMessage.of(
            EMsg.k_EMsgClientGamesPlayed_VALUE,
            MsgHeaderProto.of(EMsg.k_EMsgClientGamesPlayed_VALUE, header).serialize(),
            proto.toByteArray());

    log.info("Sending games played message: {}", message);

    write(message);
  }

  @Override
  public <H extends BaseMsgHeader, T> void write(BaseMsg<H, T> msg) {
    super.write(msg);
  }
}
