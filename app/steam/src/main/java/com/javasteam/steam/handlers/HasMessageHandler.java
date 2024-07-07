package com.javasteam.steam.handlers;

import com.javasteam.models.AbstractMessage;
import com.javasteam.models.Header;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
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

  default <H extends Header, T> MessageHandler.MessageListenerItem<H, T> addMessageListener(
      MessageHandler.MessageListenerItem<H, T> item) {
    return (MessageHandler.MessageListenerItem<H, T>) getMessageHandler().addMessageListener(item);
  }

  default <H extends Header, T> MessageHandler.MessageListenerItem<H, T> addMessageListener(
      Integer emsg, int priority, Consumer<AbstractMessage<H, T>> consumer) {
    return addMessageListener(MessageHandler.MessageListenerItem.of(emsg, priority, consumer));
  }

  default <H extends Header, T> MessageHandler.MessageListenerItem<H, T> addMessageListener(
      Integer emsg, Consumer<AbstractMessage<H, T>> consumer) {
    return addMessageListener(
        MessageHandler.MessageListenerItem.of(emsg, MessageHandler.DEFAULT_PRIORITY, consumer));
  }

  default <H extends Header, T> MessageHandler.MessageFutureItem<H, T> addMessageFuture(
      MessageHandler.MessageFutureItem<H, T> item) {
    return (MessageHandler.MessageFutureItem<H, T>) getMessageHandler().addMessageFuture(item);
  }

  default <H extends Header, T> AbstractMessage<H, T> waitForMessage(
      Integer emsg, long timeoutMs, int priority) throws TimeoutException {
    try {
      return addMessageFuture(MessageHandler.MessageFutureItem.<H, T>of(emsg, priority))
          .getFuture()
          .get(timeoutMs, TimeUnit.MILLISECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException exception) {
      throw new TimeoutException("Timeout waiting for message %d".formatted(emsg));
    }
  }

  default <H extends Header, T> AbstractMessage<H, T> waitForMessage(Integer emsg, long timeoutMs)
      throws TimeoutException {
    try {
      return addMessageFuture(
              MessageHandler.MessageFutureItem.<H, T>of(emsg, MessageHandler.DEFAULT_PRIORITY))
          .getFuture()
          .get(timeoutMs, TimeUnit.MILLISECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException exception) {
      throw new TimeoutException("Timeout waiting for message %d".formatted(emsg));
    }
  }

  default <H extends Header, T> AbstractMessage<H, T> waitForMessage(Integer emsg, int priority) {
    return addMessageFuture(MessageHandler.MessageFutureItem.<H, T>of(emsg, priority))
        .getFuture()
        .join();
  }

  default <H extends Header, T> AbstractMessage<H, T> waitForMessage(Integer emsg) {
    return addMessageFuture(
            MessageHandler.MessageFutureItem.<H, T>of(emsg, MessageHandler.DEFAULT_PRIORITY))
        .getFuture()
        .join();
  }

  default <H extends Header, T> void notifyMessageListeners(AbstractMessage<H, T> message) {
    getMessageHandler().notifyListeners(message.getEMsg(), message);
  }
}
