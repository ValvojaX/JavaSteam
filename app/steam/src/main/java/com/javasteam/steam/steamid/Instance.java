package com.javasteam.steam.steamid;

import java.util.Arrays;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents the instance of a Steam ID.
 *
 * @see <a href="https://partner.steamgames.com/doc/api/steam_api#CSteamID">Steam documentation -
 *     SteamID</a>
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum Instance {
  ALL(0),
  DESKTOP(1),
  CONSOLE(2),
  WEB(4);

  private final int code;

  public static Instance fromCode(int code) {
    return Arrays.stream(values())
        .filter(instance -> instance.code == code)
        .findFirst()
        .orElse(ALL);
  }
}
