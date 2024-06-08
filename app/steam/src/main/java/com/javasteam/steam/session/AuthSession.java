package com.javasteam.steam.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.javasteam.utils.common.JsonUtils;
import com.javasteam.utils.common.StorageUtils;
import java.util.Base64;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** Holds the authentication session information. */
@Getter
@Setter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthSession {
  @JsonProperty("username")
  private String username;

  @JsonProperty("access_token")
  private String accessToken;

  @JsonProperty("refresh_token")
  private String refreshToken;

  @JsonProperty("machine_id")
  private String machineId;

  @JsonIgnore private Consumer<byte[]> sessionSaver;

  @JsonCreator
  private AuthSession(
      @JsonProperty("username") String username,
      @JsonProperty("access_token") String accessToken,
      @JsonProperty("refresh_token") String refreshToken,
      @JsonProperty("machine_id") String machineId) {
    this.username = username;
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.machineId = machineId;
  }

  public static AuthSession fromFile(String sessionFilePath, String password) {
    var session =
        JsonUtils.fromJson(
            StorageUtils.readEncryptedFile(sessionFilePath, password), AuthSession.class);

    session.setSessionSaver(
        bytes -> StorageUtils.saveEncryptedFile(sessionFilePath, password, bytes));

    return session;
  }

  @JsonIgnore
  public long getSteamIdFromRefreshToken() {
    return Long.parseLong(parseRefreshToken().getSubject());
  }

  @JsonIgnore
  public byte[] getMachineId() {
    return Base64.getDecoder().decode(machineId);
  }

  @JsonIgnore
  public boolean isRefreshTokenExpired() {
    return parseRefreshToken().getExpiration() < System.currentTimeMillis() / 1000;
  }

  @JsonIgnore
  public boolean isAccessTokenExpired() {
    return parseAccessToken().getExpiration() < System.currentTimeMillis() / 1000;
  }

  private AuthSessionRefreshToken parseRefreshToken() {
    return JsonUtils.fromJson(parseJsonPart(refreshToken), AuthSessionRefreshToken.class);
  }

  private AuthSessionAccessToken parseAccessToken() {
    return JsonUtils.fromJson(parseJsonPart(accessToken), AuthSessionAccessToken.class);
  }

  private String parseJsonPart(String jwtToken) {
    String jsonPart = jwtToken.split("\\.")[1];
    if (jsonPart.length() % 4 != 0) {
      jsonPart += "=".repeat(4 - jsonPart.length() % 4);
    }
    return new String(Base64.getDecoder().decode(jsonPart));
  }

  @Override
  public String toString() {
    return "AuthSession(\n"
        + "  username = %s\n".formatted(username)
        + "  accessToken = %s\n".formatted(accessToken)
        + "  refreshToken = %s\n".formatted(refreshToken)
        + "  machineId = %s\n".formatted(machineId)
        + ")";
  }
}
