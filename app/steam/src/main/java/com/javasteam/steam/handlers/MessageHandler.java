package com.javasteam.steam.handlers;

import com.javasteam.handlers.BaseMessageHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * Steam message listener container where callback listeners can be added to. This class is used to
 * manage the listeners for a connection.
 */
@Slf4j
public class MessageHandler extends BaseMessageHandler<Integer> {
  public MessageHandler() {
    super();
  }

  public MessageHandler(int threads) {
    super(threads);
  }
}
