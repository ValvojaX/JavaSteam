package com.javasteam.steam.handlers;

import com.javasteam.models.AbstractMessage;
import com.javasteam.models.Header;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Helper interface to delegate {@link MessageHandler} methods. Implement either of the methods
 * {@link #getMessageHandler()} or {@link #getInstance()} to delegate the methods.
 */
public interface HasMessageHandler {
  /** Implement this method if you have access to the {@link MessageHandler} object */
  default MessageHandler getMessageHandler() {
    return getInstance().getMessageHandler();
  }

  /** Implement this if you have access to an object that implements {@link HasMessageHandler} */
  default HasMessageHandler getInstance() {
    return null;
  }

  default MessageHandler.MessageHandlerItem addMessageListener(
      MessageHandler.MessageHandlerItem item) {
    return getMessageHandler().addMessageListener(item);
  }

  default <H extends Header, T> void addMessageListener(
      int emsg, Consumer<AbstractMessage<H, T>> listener) {
    getMessageHandler().addMessageListener(emsg, listener);
  }

  default <H extends Header, T> void waitForMessage(
      int emsg, Consumer<AbstractMessage<H, T>> listener) {
    getMessageHandler().waitForMessage(emsg, listener);
  }

  default <H extends Header, T> void waitForMessage(
      int emsg, Consumer<AbstractMessage<H, T>> listener, long timeoutMs) throws TimeoutException {
    getMessageHandler().waitForMessage(emsg, listener, timeoutMs);
  }

  default void waitForMessage(int emsg) {
    getMessageHandler().waitForMessage(emsg);
  }

  default void waitForMessage(int emsg, long timeoutMs) throws TimeoutException {
    getMessageHandler().waitForMessage(emsg, timeoutMs);
  }

  default <H extends Header> void notifyMessageListeners(AbstractMessage<H, Object> message) {
    getMessageHandler().notifyMessageListeners(message);
  }
}
