package com.javasteam.steam;

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
  private SteamId steamId;
  private Integer sessionId;

  public Optional<SteamId> getSteamIdOptional() {
    return Optional.ofNullable(steamId);
  }
}
