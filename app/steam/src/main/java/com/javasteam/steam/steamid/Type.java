package com.javasteam.steam.steamid;

import java.util.Arrays;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents the type of Steam ID.
 *
 * @see <a href="https://partner.steamgames.com/doc/api/steam_api#EAccountType">Steam documentation
 *     - SteamID</a>
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum Type {
  INVALID(0, "I"),
  INDIVIDUAL(1, "U"),
  MULTISEAT(2, "M"),
  GAMESERVER(3, "G"),
  ANON_GAMESERVER(4, "A"),
  PENDING(5, "P"),
  CONTENT_SERVER(6, "C"),
  CLAN(7, "g"),
  CHAT(8, "T"),
  ANON_USER(10, "a");

  private final int code;
  private final String prefix;

  public static Type fromCode(int code) {
    return Arrays.stream(values()).filter(type -> type.code == code).findFirst().orElse(INVALID);
  }
}
