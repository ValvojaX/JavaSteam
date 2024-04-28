package com.javasteam.steam;

import com.javasteam.models.AbstractMessage;
import com.javasteam.models.Header;
import com.javasteam.steam.connection.ListenerGroup;
import java.util.function.Consumer;

/**
 * Helper interface to delegate {@link ListenerGroup} methods. Implement either of the methods
 * {@link #getListenerGroup()} or {@link #getInstance()} to delegate the methods.
 */
public interface HasListenerGroup {
  /** Implement this method if you have access to the {@link ListenerGroup} object */
  default ListenerGroup getListenerGroup() {
    return getInstance().getListenerGroup();
  }

  /** Implement this if you have access to an object that implements {@link HasListenerGroup} */
  default HasListenerGroup getInstance() {
    return null;
  }

  default <H extends Header, T> void addMessageListener(
      int emsg, Consumer<AbstractMessage<H, T>> listener) {
    getListenerGroup().addMessageListener(emsg, listener);
  }

  default <H extends Header, T> void waitForMessage(
      int emsg, Consumer<AbstractMessage<H, T>> listener) {
    getListenerGroup().waitForMessage(emsg, listener);
  }

  default void waitForMessage(int emsg) {
    getListenerGroup().waitForMessage(emsg);
  }

  default <H extends Header> void notifyMessageListeners(AbstractMessage<H, Object> message) {
    getListenerGroup().notifyMessageListeners(message);
  }
}
