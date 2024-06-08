package com.javasteam.steam.session;

import static com.javasteam.protobufs.Enums.ESessionPersistence;
import static com.javasteam.protobufs.EnumsClientserver.EMsg;
import static com.javasteam.protobufs.SteammessagesAuthSteamclient.CAuthentication_AccessToken_GenerateForApp_Request;
import static com.javasteam.protobufs.SteammessagesAuthSteamclient.CAuthentication_AccessToken_GenerateForApp_Response;
import static com.javasteam.protobufs.SteammessagesAuthSteamclient.CAuthentication_BeginAuthSessionViaCredentials_Request;
import static com.javasteam.protobufs.SteammessagesAuthSteamclient.CAuthentication_BeginAuthSessionViaCredentials_Response;
import static com.javasteam.protobufs.SteammessagesAuthSteamclient.CAuthentication_DeviceDetails;
import static com.javasteam.protobufs.SteammessagesAuthSteamclient.CAuthentication_GetPasswordRSAPublicKey_Request;
import static com.javasteam.protobufs.SteammessagesAuthSteamclient.CAuthentication_GetPasswordRSAPublicKey_Response;
import static com.javasteam.protobufs.SteammessagesAuthSteamclient.CAuthentication_PollAuthSessionStatus_Request;
import static com.javasteam.protobufs.SteammessagesAuthSteamclient.CAuthentication_PollAuthSessionStatus_Response;
import static com.javasteam.protobufs.SteammessagesAuthSteamclient.EAuthTokenPlatformType;
import static com.javasteam.protobufs.SteammessagesAuthSteamclient.ETokenRenewalType;
import static com.javasteam.protobufs.SteammessagesBase.CMsgProtoBufHeader;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.javasteam.models.Job;
import com.javasteam.models.headers.ProtoMessageHeader;
import com.javasteam.models.messages.ProtoMessage;
import com.javasteam.steam.crypto.Crypto;
import com.javasteam.steam.handlers.HasJobHandler;
import com.javasteam.steam.handlers.HasJobSender;
import com.javasteam.utils.common.CryptoUtils;
import com.javasteam.utils.common.JsonUtils;
import com.javasteam.utils.common.StorageUtils;
import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Creates an auth session using jobs. */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthSessionService<T extends HasJobSender & HasJobHandler> {
  private final T client;
  private final AuthSession.AuthSessionBuilder authSessionBuilder;
  private final Consumer<AuthSession> onAuthSessionCreated;

  public static <T extends HasJobSender & HasJobHandler> AuthSessionService<T> of(
      T client, Consumer<AuthSession> onAuthSessionCreated) {
    return new AuthSessionService<>(client, AuthSession.builder(), onAuthSessionCreated);
  }

  public void createAuthSession(String username, String password, String sessionSaveFilePath) {
    var message =
        CAuthentication_GetPasswordRSAPublicKey_Request.newBuilder()
            .setAccountName(username)
            .build();

    authSessionBuilder.sessionSaver(
        bytes -> StorageUtils.saveEncryptedFile(sessionSaveFilePath, password, bytes));

    Job job =
        client.sendJob(
            ProtoMessage.of(createServiceMethodCallHeader(), message),
            Job.of("Authentication.GetPasswordRSAPublicKey#1", 1));

    client.addJobListener(
        job.getSourceJobId(),
        bytes -> {
          try {
            return CAuthentication_GetPasswordRSAPublicKey_Response.parseFrom(bytes);
          } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
          }
        },
        res -> onGetPasswordRSAPublicKeyResponse(res, username, password));
  }

  public void updateAccessToken(AuthSession authSession, boolean renewRefreshToken) {
    var message =
        CAuthentication_AccessToken_GenerateForApp_Request.newBuilder()
            .setRefreshToken(authSession.getRefreshToken())
            .setSteamid(authSession.getSteamIdFromRefreshToken())
            .setRenewalType(
                renewRefreshToken
                    ? ETokenRenewalType.k_ETokenRenewalType_Allow
                    : ETokenRenewalType.k_ETokenRenewalType_None)
            .build();

    log.debug("Updating access token for user: {}", authSession.getUsername());

    Job job =
        client.sendJob(
            ProtoMessage.of(createServiceMethodCallHeader(), message),
            Job.of("Authentication.GenerateAccessTokenForApp#1", 1));

    client.waitForJob(
        job.getSourceJobId(),
        bytes -> {
          try {
            return CAuthentication_AccessToken_GenerateForApp_Response.parseFrom(bytes);
          } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
          }
        },
        response -> {
          log.debug("Access token updated for user: {}", authSession.getUsername());
          if (response.hasRefreshToken()) {
            authSession.setRefreshToken(response.getRefreshToken());
          }

          authSession.setAccessToken(response.getAccessToken());
          saveAuthSession(authSession);
        });
  }

  public void saveAuthSession(AuthSession authSession) {
    if (authSession.getSessionSaver() == null) {
      throw new IllegalStateException("Session saver is not set");
    }
    authSession.getSessionSaver().accept(JsonUtils.toJson(authSession).getBytes());
  }

  private void onGetPasswordRSAPublicKeyResponse(
      CAuthentication_GetPasswordRSAPublicKey_Response response, String username, String password) {
    BigInteger modulus = new BigInteger(response.getPublickeyMod(), 16);
    BigInteger exponent = new BigInteger(response.getPublickeyExp(), 16);

    RSAPublicKey rsaPublicKey = Crypto.createRSAPublicKey(modulus, exponent);
    byte[] encryptedPassword = CryptoUtils.encryptRSA(password.getBytes(), rsaPublicKey);
    byte[] machineId = Crypto.generateMachineID();

    authSessionBuilder.username(username).machineId(Base64.getEncoder().encodeToString(machineId));

    CAuthentication_BeginAuthSessionViaCredentials_Request request =
        CAuthentication_BeginAuthSessionViaCredentials_Request.newBuilder()
            .setAccountName(username)
            .setEncryptedPassword(Base64.getEncoder().encodeToString(encryptedPassword))
            .setEncryptionTimestamp(response.getTimestamp())
            .setRememberLogin(true)
            .setPersistence(ESessionPersistence.k_ESessionPersistence_Persistent)
            .setDeviceDetails(
                CAuthentication_DeviceDetails.newBuilder()
                    .setMachineId(ByteString.copyFrom(machineId))
                    .setPlatformType(EAuthTokenPlatformType.k_EAuthTokenPlatformType_SteamClient)
                    .build())
            .build();

    Job job =
        client.sendJob(
            ProtoMessage.of(createServiceMethodCallHeader(), request),
            Job.of("Authentication.BeginAuthSessionViaCredentials#1", 1));

    client.addJobListener(
        job.getSourceJobId(),
        bytes -> {
          try {
            return CAuthentication_BeginAuthSessionViaCredentials_Response.parseFrom(bytes);
          } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
          }
        },
        this::onAuthSessionResponse);
  }

  private void onAuthSessionResponse(
      CAuthentication_BeginAuthSessionViaCredentials_Response response) {
    var message =
        CAuthentication_PollAuthSessionStatus_Request.newBuilder()
            .setClientId(response.getClientId())
            .setRequestId(response.getRequestId())
            .build();

    Job job =
        client.sendJob(
            ProtoMessage.of(createServiceMethodCallHeader(), message),
            Job.of("Authentication.PollAuthSessionStatus#1", 1));

    client.addJobListener(
        job.getSourceJobId(),
        bytes -> {
          try {
            return CAuthentication_PollAuthSessionStatus_Response.parseFrom(bytes);
          } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
          }
        },
        this::onPollAuthSessionStatusResponse);
  }

  private void onPollAuthSessionStatusResponse(
      CAuthentication_PollAuthSessionStatus_Response response) {
    AuthSession authSession =
        authSessionBuilder
            .accessToken(response.getAccessToken())
            .refreshToken(response.getRefreshToken())
            .build();

    log.debug("Received auth session response: {}", response);
    saveAuthSession(authSession);
    onAuthSessionCreated.accept(authSession);
  }

  private ProtoMessageHeader createServiceMethodCallHeader() {
    return ProtoMessageHeader.of(
        EMsg.k_EMsgServiceMethodCallFromClient_VALUE, CMsgProtoBufHeader.newBuilder().build());
  }
}
