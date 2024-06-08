package com.javasteam.steam.session;

import com.javasteam.steam.steamid.SteamId;
import java.util.Optional;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SteamSessionContext is a class that holds the SteamId of the user that is currently logged in.
 */
@Getter
@Setter
@NoArgsConstructor
public class SteamSessionContext {
  private String username;
  private SteamId steamId;
  private Integer sessionId;
  private AuthSession authSession;

  public Optional<SteamId> getSteamIdOptional() {
    return Optional.ofNullable(steamId);
  }

  public Optional<Integer> getSessionIdOptional() {
    return Optional.ofNullable(sessionId);
  }

  public Optional<AuthSession> getAuthSession() {
    return Optional.ofNullable(authSession);
  }
}
