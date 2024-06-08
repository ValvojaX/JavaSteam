package com.javasteam.models;

/** Marks a header to contain information about the session context. */
public interface HasSessionContext {
  void setSessionId(Integer sessionId);

  void setSteamId(long steamId);
}
