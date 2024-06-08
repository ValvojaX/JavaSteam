package com.javasteam.steam;

import static com.javasteam.protobufs.SteammessagesClientserverLogin.CMsgClientLogon;

import com.google.protobuf.ByteString;
import com.javasteam.steam.common.EResult;
import com.javasteam.steam.session.AuthSession;
import com.javasteam.utils.common.StorageUtils;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Helper class to support different login parameters for steam clients. */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginParameters {
  private final Consumer<CMsgClientLogon.Builder> logonBuilderConsumer;
  @Getter private AuthSession authSession;
  @Getter private final String authSessionSaveFilePath;

  public CMsgClientLogon.Builder apply(CMsgClientLogon.Builder logonBuilder) {
    logonBuilderConsumer.accept(logonBuilder);
    return logonBuilder;
  }

  public static LoginParameters with(
      String username, String password, boolean shouldRememberPassword) {
    return new LoginParameters(
        builder -> {
          builder.setAccountName(username);
          builder.setPassword(password);
          builder.setShouldRememberPassword(shouldRememberPassword);
        },
        getDefaultSessionFilePath(username));
  }

  public static LoginParameters with(String username, String password) {
    return new LoginParameters(
        builder -> {
          builder.setAccountName(username);
          builder.setPassword(password);
          builder.setShouldRememberPassword(false);
        },
        getDefaultSessionFilePath(username));
  }

  public static LoginParameters withSessionFile(String username, String password) {
    String sessionFilePath = getDefaultSessionFilePath(username);
    AuthSession authSession = AuthSession.fromFile(sessionFilePath, password);
    return new LoginParameters(
        builder -> {
          builder.setAccessToken(authSession.getRefreshToken());
          builder.setMachineId(ByteString.copyFrom(authSession.getMachineId()));
        },
        authSession,
        sessionFilePath);
  }

  public static LoginParameters withSessionFile(
      String username, String password, String sessionFilePath) {
    AuthSession authSession = AuthSession.fromFile(sessionFilePath, password);
    return new LoginParameters(
        builder -> {
          builder.setAccessToken(authSession.getAccessToken());
          builder.setMachineId(ByteString.copyFrom(authSession.getMachineId()));
        },
        authSession,
        sessionFilePath);
  }

  public static LoginParameters withLoginKey(String username, String loginKey) {
    return new LoginParameters(
        builder -> {
          builder.setAccountName(username);
          builder.setLoginKey(loginKey);
        },
        getDefaultSessionFilePath(username));
  }

  public static LoginParameters withRefreshToken(
      String username, String accessToken, ByteString machineId) {
    return new LoginParameters(
        builder -> {
          builder.setAccountName(username);
          builder.setAccessToken(accessToken);
          builder.setMachineId(machineId);
        },
        getDefaultSessionFilePath(username));
  }

  public static LoginParameters withSentryFileHash(
      String username, String password, ByteString sentryFileHash) {
    return new LoginParameters(
        builder -> {
          builder.setAccountName(username);
          builder.setPassword(password);
          builder.setShaSentryfile(sentryFileHash);
          builder.setEresultSentryfile(EResult.FileNotFound);
        },
        getDefaultSessionFilePath(username));
  }

  public static LoginParameters withTwoFactorCode(
      String username, String password, String twoFactorCode, boolean shouldRememberPassword) {
    return new LoginParameters(
        builder -> {
          builder.setAccountName(username);
          builder.setPassword(password);
          builder.setTwoFactorCode(twoFactorCode);
          builder.setShouldRememberPassword(shouldRememberPassword);
        },
        getDefaultSessionFilePath(username));
  }

  public static LoginParameters withTwoFactorCode(
      String username, String password, String twoFactorCode) {
    return new LoginParameters(
        builder -> {
          builder.setAccountName(username);
          builder.setPassword(password);
          builder.setTwoFactorCode(twoFactorCode);
          builder.setShouldRememberPassword(false);
        },
        getDefaultSessionFilePath(username));
  }

  public static LoginParameters withAuthCode(
      String username, String password, String authCode, boolean shouldRememberPassword) {
    return new LoginParameters(
        builder -> {
          builder.setAccountName(username);
          builder.setPassword(password);
          builder.setAuthCode(authCode);
          builder.setShouldRememberPassword(shouldRememberPassword);
        },
        getDefaultSessionFilePath(username));
  }

  public static LoginParameters withAuthCode(String username, String password, String authCode) {
    return new LoginParameters(
        builder -> {
          builder.setAccountName(username);
          builder.setPassword(password);
          builder.setAuthCode(authCode);
          builder.setShouldRememberPassword(false);
        },
        getDefaultSessionFilePath(username));
  }

  private static String getDefaultSessionFilePath(String username) {
    return "%s/.javasteam/sessions/%s.bin".formatted(StorageUtils.getHomeDirectory(), username);
  }
}
