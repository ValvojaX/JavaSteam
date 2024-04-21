package com.javasteam.steam.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents the persona state of a user.
 *
 * @see <a href=https://partner.steamgames.com/doc/api/ISteamFriends#EPersonaState>EPersonaState</a>
 */
@Getter
@AllArgsConstructor
public enum EPersonaState {
  OFFLINE(0),
  ONLINE(1),
  BUSY(2),
  AWAY(3),
  SNOOZE(4),
  LOOKING_TO_TRADE(5),
  LOOKING_TO_PLAY(6),
  MAX(7);

  private final int code;
}
