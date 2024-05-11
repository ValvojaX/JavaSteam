package com.javasteam.steam;

import static com.javasteam.protobufs.EnumsClientserver.EMsg;
import static com.javasteam.protobufs.SteammessagesBase.CMsgIPAddress;
import static com.javasteam.protobufs.SteammessagesBase.CMsgProtoBufHeader;
import static com.javasteam.protobufs.SteammessagesClientserver.CMsgClientGamesPlayed;
import static com.javasteam.protobufs.SteammessagesClientserver2.CMsgClientServiceCall;
import static com.javasteam.protobufs.SteammessagesClientserver2.CMsgClientServiceCallResponse;
import static com.javasteam.protobufs.SteammessagesClientserverFriends.CMsgClientChangeStatus;
import static com.javasteam.protobufs.SteammessagesClientserverFriends.CMsgClientPersonaState;
import static com.javasteam.protobufs.SteammessagesClientserverLogin.CMsgClientLogon;
import static com.javasteam.protobufs.SteammessagesClientserverLogin.CMsgClientLogonResponse;

import com.google.protobuf.GeneratedMessage;
import com.javasteam.models.AbstractMessage;
import com.javasteam.models.HasJob;
import com.javasteam.models.HasSessionContext;
import com.javasteam.models.Header;
import com.javasteam.models.Job;
import com.javasteam.models.ProtoHeader;
import com.javasteam.models.headers.ProtoMessageHeader;
import com.javasteam.models.messages.ProtoMessage;
import com.javasteam.steam.common.EPersonaState;
import com.javasteam.steam.common.EResult;
import com.javasteam.steam.steamid.SteamId;
import com.javasteam.steam.steamid.Type;
import com.javasteam.steam.steamid.Universe;
import com.javasteam.utils.serializer.Serializer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.extern.slf4j.Slf4j;

/**
 * Steam client that connects to a CM server and listens for messages. Handles the client logon
 * process and sends heartbeats to the CM server. The client can be used to send and receive
 * messages from the CM server.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * SteamClient client = new SteamClient();
 * client.login(username, password);
 * client.addMessageListener(EMsg.k_EMsgClientLogon, msg -> {
 *     // Handle client logon message
 * });
 * }</pre>
 */
@Slf4j
public class SteamClient extends SteamCMClient implements HasJobHandler {
  private static final int DEFAULT_THREADS = 10;
  private SteamSessionContext sessionContext;
  private final ScheduledExecutorService executor;
  private final JobHandler jobHandler;

  public SteamClient(int threads) {
    super(threads);
    this.addMessageListeners();
    this.jobHandler = new JobHandler(threads);
    this.sessionContext = new SteamSessionContext();
    this.executor = Executors.newSingleThreadScheduledExecutor();
  }

  public SteamClient() {
    this(DEFAULT_THREADS);
  }

  private void addMessageListeners() {
    this.addMessageListener(EMsg.k_EMsgClientLogOnResponse_VALUE, this::onClientLogonResponse);
    this.addMessageListener(EMsg.k_EMsgClientPersonaState_VALUE, this::onClientPersonaState);
    this.addMessageListener(EMsg.k_EMsgServiceMethodResponse_VALUE, this::onServiceMethodResponse);
    this.addMessageListener(EMsg.k_EMsgClientServiceCall_VALUE, this::onClientServiceCall);
    this.addMessageListener(
        EMsg.k_EMsgClientServiceCallResponse_VALUE, this::onClientServiceCallResponse);
  }

  private void onClientPersonaState(
      AbstractMessage<ProtoMessageHeader, CMsgClientPersonaState> msg) {
    log.info("Received client persona state:\n{}", msg);
  }

  private void onServiceMethodResponse(AbstractMessage<ProtoMessageHeader, byte[]> msg) {
    log.info("Received service method response:\n{}", msg);
    var headerProto = msg.getMsgHeader().getProto();
    var bodyBytes = msg.getMsgBody(bytes -> bytes);
    this.jobHandler.onJob(headerProto.getJobidTarget(), bodyBytes);
  }

  private void onClientServiceCall(AbstractMessage<ProtoMessageHeader, CMsgClientServiceCall> msg) {
    log.info("Received client service call:\n{}", msg);
  }

  private void onClientServiceCallResponse(
      AbstractMessage<ProtoMessageHeader, CMsgClientServiceCallResponse> msg) {
    log.info("Received client service call response:\n{}", msg);
  }

  public void onClientLogonResponse(
      AbstractMessage<ProtoMessageHeader, CMsgClientLogonResponse> msg) {
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

    CMsgClientLogonResponse response = CMsgClientLogonResponse.newBuilder().build();

    CMsgProtoBufHeader header = CMsgProtoBufHeader.newBuilder().build();

    var message =
        ProtoMessage.of(ProtoMessageHeader.of(EMsg.k_EMsgClientHeartBeat_VALUE, header), response);
    this.sendMessage(message);
  }

  public void loginAnonymous() {
    if (this.isConnected()) {
      throw new RuntimeException("Client is already connected");
    }

    this.connect();
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

    var message =
        ProtoMessage.of(
            ProtoMessageHeader.of(EMsg.k_EMsgClientLogon_VALUE, headerProto), logonMessage);

    log.info("Sending client logon message:\n{}", message);
    this.sendMessage(message);
    this.waitForMessage(EMsg.k_EMsgClientLogOnResponse_VALUE);
  }

  public void login(LoginParameters loginParameters) {
    if (this.isConnected()) {
      throw new RuntimeException("Client is already connected");
    }

    this.connect();
    SteamId steamId = SteamId.of(Universe.PUBLIC, Type.INDIVIDUAL);
    this.sessionContext.setSteamId(steamId);

    int ipv4Address =
        Serializer.unpack(getLocalAddress().getAddress(), ByteBuffer::getInt, ByteOrder.BIG_ENDIAN);
    int obfuscatedAddress = ipv4Address ^ SteamProtocol.ADDRESS_MASK;

    var logonMessage =
        loginParameters
            .apply(
                CMsgClientLogon.newBuilder()
                    .setClientPackageVersion(SteamProtocol.DEFAULT_CLIENT_PACKAGE_VERSION)
                    .setProtocolVersion(SteamProtocol.DEFAULT_PROTOCOL_VERSION)
                    .setObfuscatedPrivateIp(
                        CMsgIPAddress.newBuilder().setV4(obfuscatedAddress).build()))
            .build();

    log.info(
        "Logging in with username: {} and Steam ID: {}", logonMessage.getAccountName(), steamId);

    CMsgProtoBufHeader headerProto =
        CMsgProtoBufHeader.newBuilder().setSteamid(steamId.toSteamId64()).build();

    var message =
        ProtoMessage.of(
            ProtoMessageHeader.of(EMsg.k_EMsgClientLogon_VALUE, headerProto), logonMessage);

    log.info("Sending client logon message:\n{}", message);
    this.sendMessage(message);
    this.waitForMessage(EMsg.k_EMsgClientLogOnResponse_VALUE);
  }

  public void setState(EPersonaState state) {
    var proto = CMsgClientChangeStatus.newBuilder().setPersonaState(state.getCode()).build();

    var header = CMsgProtoBufHeader.newBuilder(CMsgProtoBufHeader.getDefaultInstance()).build();

    var message =
        ProtoMessage.of(ProtoMessageHeader.of(EMsg.k_EMsgClientChangeStatus_VALUE, header), proto);

    log.info("Sending change status message: {}", message);
    sendMessage(message);
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

    var header = CMsgProtoBufHeader.newBuilder(CMsgProtoBufHeader.getDefaultInstance()).build();

    var message =
        ProtoMessage.of(ProtoMessageHeader.of(EMsg.k_EMsgClientGamesPlayed_VALUE, header), proto);

    log.info("Sending games played message: {}", message);

    sendMessage(message);
  }

  public <H extends ProtoHeader, T extends GeneratedMessage> void sendMessage(
      ProtoMessage<H, T> msg) {
    if (msg.getMsgHeader() instanceof HasSessionContext header) {
      sessionContext
          .getSteamIdOptional()
          .ifPresent(steamId -> header.setSteamId(steamId.toSteamId64()));
      sessionContext.getSessionIdOptional().ifPresent(header::setSessionId);
    }
    super.sendMessage(msg);
  }

  public synchronized <H extends Header & HasJob> Job sendJob(
      AbstractMessage<H, ?> message, Job job) {
    job.setSourceJobId(getJobHandler().getNextJobId());
    message.getMsgHeader().setJob(job);
    sendMessage(message);
    return job;
  }

  @Override
  public boolean isConnected() {
    return super.isConnected() && sessionContext.getSteamIdOptional().isPresent();
  }

  @Override
  public void disconnect() {
    super.disconnect();
    this.sessionContext = new SteamSessionContext();
  }

  @Override
  public JobHandler getJobHandler() {
    return jobHandler;
  }
}
