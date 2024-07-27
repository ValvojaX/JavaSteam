package com.javasteam.steam.handlers;

import com.javasteam.handlers.FutureItem;
import com.javasteam.handlers.ListenerItem;
import com.javasteam.models.AbstractMessage;
import com.javasteam.models.Header;
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

  default <R, T> ListenerItem<Integer, R, T> addMessageListener(ListenerItem<Integer, R, T> item) {
    return getMessageHandler().addMessageListener(item);
  }

  default <R, T> T waitForMessage(FutureItem<Integer, R, T> item) {
    return getMessageHandler().addMessageFuture(item);
  }

  default <H extends Header, T> void notifyMessageListeners(AbstractMessage<H, T> message) {
    getMessageHandler().notifyListeners(message.getEMsg(), message);
  }

  /** Commonly used method to add a message listener. */
  default <H extends Header, T> void addMessageListener(
      Integer id, Consumer<AbstractMessage<H, T>> consumer) {
    getMessageHandler().addMessageListener(ListenerItem.builder(id, consumer).build());
  }

  /** Commonly used method to wait for a message. */
  @SuppressWarnings("unchecked")
  default <H extends Header, T> AbstractMessage<H, T> waitForMessage(Integer id) {
    return (AbstractMessage<H, T>)
        getMessageHandler().addMessageFuture(FutureItem.builder(id).build());
  }
}
