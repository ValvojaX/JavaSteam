package com.javasteam.steam;

import static com.javasteam.protobufs.SteammessagesClientserverLogin.CMsgClientLogon;

import com.google.protobuf.ByteString;
import com.javasteam.steam.common.EResult;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/** Helper class to support different login parameters for steam clients. */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginParameters {
  private Consumer<CMsgClientLogon.Builder> logonBuilderConsumer;

  public CMsgClientLogon.Builder apply(CMsgClientLogon.Builder logonBuilder) {
    logonBuilderConsumer.accept(logonBuilder);
    return logonBuilder;
  }

  public static LoginParameters with(String username, String password) {
    return new LoginParameters(
        builder -> {
          builder.setAccountName(username);
          builder.setPassword(password);
        });
  }

  public static LoginParameters withLoginKey(String username, String loginKey) {
    return new LoginParameters(
        builder -> {
          builder.setAccountName(username);
          builder.setLoginKey(loginKey);
        });
  }

  public static LoginParameters withSentryFileHash(
      String username, String password, ByteString sentryFileHash) {
    return new LoginParameters(
        builder -> {
          builder.setAccountName(username);
          builder.setPassword(password);
          builder.setShaSentryfile(sentryFileHash);
          builder.setEresultSentryfile(EResult.FileNotFound);
        });
  }

  public static LoginParameters withTwoFactorCode(
      String username, String password, String twoFactorCode) {
    return new LoginParameters(
        builder -> {
          builder.setAccountName(username);
          builder.setPassword(password);
          builder.setTwoFactorCode(twoFactorCode);
        });
  }

  public static LoginParameters withAuthCode(String username, String password, String authCode) {
    return new LoginParameters(
        builder -> {
          builder.setAccountName(username);
          builder.setPassword(password);
          builder.setAuthCode(authCode);
        });
  }
}
