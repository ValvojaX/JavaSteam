package com.javasteam.steam;

/** Steam protocol constants. */
public class SteamProtocol {
  public static final int PACKET_MAGIC = 0x31305456; // b'VT01' in little-endian
  public static final int PACKET_HEADER_SIZE = 8;
  public static final int DEFAULT_CLIENT_PACKAGE_VERSION = 1561159470;
  public static final int DEFAULT_PROTOCOL_VERSION = 65580;
  public static final int ADDRESS_MASK = 0xF00DBAAD;

  private SteamProtocol() {}
}
