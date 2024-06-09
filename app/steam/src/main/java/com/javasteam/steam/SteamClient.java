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
import com.javasteam.steam.common.SteamProtocol;
import com.javasteam.steam.handlers.HasJobHandler;
import com.javasteam.steam.handlers.HasJobSender;
import com.javasteam.steam.handlers.JobHandler;
import com.javasteam.steam.session.AuthSession;
import com.javasteam.steam.session.AuthSessionService;
import com.javasteam.steam.session.SteamSessionContext;
import com.javasteam.steam.steamid.SteamId;
import com.javasteam.steam.steamid.Type;
import com.javasteam.steam.steamid.Universe;
import com.javasteam.utils.serializer.Serializer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * Steam client for interacting with Steam CM servers. Provides methods for logging in and support
 * for jobs. Also creates a session with an access token for possible web API requests.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * SteamClient client = new SteamClient();
 * client.addMessageListener(EMsg.k_EMsgClientLogon, msg -> {
 *     // Handle client logon message
 * });
 * client.login(username, password);
 * }</pre>
 */
@Slf4j
public class SteamClient extends SteamCMClient implements HasJobHandler, HasJobSender {
  private static final int DEFAULT_THREADS = 10;
  private static final int AUTH_SESSION_REFRESH_INTERVAL = 12;
  private SteamSessionContext sessionContext;
  private final ScheduledExecutorService executor;
  private final JobHandler jobHandler;
  private final AuthSessionService<SteamClient> authSessionService;

  public SteamClient(int threads) {
    super(threads);
    this.addMessageListeners();
    this.jobHandler = new JobHandler(threads);
    this.sessionContext = new SteamSessionContext();
    this.executor = Executors.newSingleThreadScheduledExecutor();
    this.authSessionService = AuthSessionService.of(this, this::onAuthSession);
    this.executor.scheduleAtFixedRate(
        this::refreshAuthSession,
        AUTH_SESSION_REFRESH_INTERVAL,
        AUTH_SESSION_REFRESH_INTERVAL,
        TimeUnit.HOURS);
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

  private void refreshAuthSession() {
    sessionContext
        .getAuthSession()
        .ifPresent(
            session -> {
              log.info("Refreshing auth session for user: {}", session.getUsername());
              authSessionService.updateAccessToken(session, true);
            });
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
    log.trace("Sending client heartbeat");

    CMsgClientLogonResponse response = CMsgClientLogonResponse.newBuilder().build();

    CMsgProtoBufHeader header = CMsgProtoBufHeader.newBuilder().build();

    var message =
        ProtoMessage.of(ProtoMessageHeader.of(EMsg.k_EMsgClientHeartBeat_VALUE, header), response);
    this.sendMessage(message);
  }

  public void loginAnonymous() {
    preLogin();

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
    preLogin();

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

    if (loginParameters.getAuthSession() != null) {
      var authSession = loginParameters.getAuthSession();
      log.info("Parsed previous auth session for user: {}", authSession.getUsername());
      this.sessionContext.setUsername(authSession.getUsername());
      this.sessionContext.setSteamId(SteamId.of(authSession.getSteamIdFromRefreshToken()));
      this.sessionContext.setAuthSession(authSession);

      if (authSession.isRefreshTokenExpired()) {
        log.warn("Refresh token in auth session of user {} is expired", authSession.getUsername());
      }
    } else {
      this.sessionContext.setUsername(logonMessage.getAccountName());
      this.sessionContext.setSteamId(SteamId.of(Universe.PUBLIC, Type.INDIVIDUAL));
    }

    log.info(
        "Logging in with username: {} and Steam ID: {}",
        this.sessionContext.getUsername(),
        this.sessionContext.getSteamId().toSteamId64());

    CMsgProtoBufHeader headerProto =
        CMsgProtoBufHeader.newBuilder()
            .setSteamid(this.sessionContext.getSteamId().toSteamId64())
            .build();

    var message =
        ProtoMessage.of(
            ProtoMessageHeader.of(EMsg.k_EMsgClientLogon_VALUE, headerProto), logonMessage);

    log.debug("Sending client logon message:\n{}", message);
    this.sendMessage(message);
    this.waitForMessage(EMsg.k_EMsgClientLogOnResponse_VALUE);

    if (shouldCreateAuthSession(logonMessage)) {
      log.info("Creating auth session for user: {}", logonMessage.getAccountName());
      authSessionService.createAuthSession(
          logonMessage.getAccountName(),
          logonMessage.getPassword(),
          loginParameters.getAuthSessionSaveFilePath());
    }
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

  private void preLogin() {
    if (this.isConnected()) {
      throw new RuntimeException("Client is already connected");
    }

    this.connect();
  }

  private void onAuthSession(AuthSession authSession) {
    log.info("Received auth session: \n{}", authSession);
    sessionContext.setAuthSession(authSession);
  }

  private boolean shouldCreateAuthSession(CMsgClientLogon logonMessage) {
    return logonMessage.hasPassword()
        && logonMessage.getShouldRememberPassword()
        && sessionContext.getAuthSession().isPresent();
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

  @Override
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
