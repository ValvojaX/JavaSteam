package com.javasteam.steam.steamid;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SteamId {
  public static final long ACCOUNT_ID_MASK = 0xFFFFFFFFL;
  public static final long ACCOUNT_INSTANCE_MASK = 0x000FFFFFL;

  private final Universe universe;
  private final Type type;
  private final Instance instance;
  private final int accountId;

  public static SteamId of(long steamId) {
    return new SteamId(
        Universe.fromCode((int) (steamId >> 56)),
        Type.fromCode((int) (steamId >> 52) & 0x0F),
        Instance.fromCode((int) (steamId >> 32) & 0xFFFFF),
        (int) steamId);
  }

  public static SteamId of(Universe universe, Type type, Instance instance) {
    return new SteamId(universe, type, instance, 0);
  }

  public static SteamId of(Universe universe, Type type) {
    return new SteamId(universe, type, Instance.DESKTOP, 0);
  }

  public long toSteamId64() {
    return ((long) universe.getCode() << 56)
        | ((long) type.getCode() << 52)
        | ((long) instance.getCode() << 32)
        | (accountId & ACCOUNT_ID_MASK);
  }

  @Override
  public String toString() {
    return "SteamId(universe=%s, type=%s, instance=%s, accountId=%s, steamId64=%s)"
        .formatted(universe, type, instance, accountId, toSteamId64());
  }
}
