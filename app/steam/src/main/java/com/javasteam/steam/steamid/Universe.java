package com.javasteam.steam.steamid;

import java.util.Arrays;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents the universe of a Steam ID.
 *
 * @see <a href="https://partner.steamgames.com/doc/api/steam_api#EUniverse">Steam documentation -
 *     SteamID</a>
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum Universe {
  INVALID(0),
  PUBLIC(1),
  BETA(2),
  INTERNAL(3),
  DEV(4),
  MAX(5);

  private final int code;

  public static Universe fromCode(int code) {
    return Arrays.stream(values())
        .filter(universe -> universe.code == code)
        .findFirst()
        .orElse(INVALID);
  }
}
