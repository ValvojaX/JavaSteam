package com.javasteam.steam.handlers;

import com.javasteam.handlers.BaseMessageHandler;
import com.javasteam.handlers.FutureItem;
import com.javasteam.handlers.ListenerItem;
import com.javasteam.models.AbstractMessage;
import com.javasteam.models.Header;
import java.util.concurrent.*;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/**
 * Steam message listener container where callback listeners can be added to. This class is used to
 * manage the listeners for a connection.
 */
@Slf4j
public class MessageHandler extends BaseMessageHandler<Integer> {
  public static final int DEFAULT_PRIORITY = 0;

  public MessageHandler() {
    super();
  }

  public MessageHandler(int threads) {
    super(threads);
  }

  public static class MessageListenerItem<H extends Header, T>
      extends ListenerItem<Integer, AbstractMessage<H, T>, AbstractMessage<H, T>> {
    public MessageListenerItem(Integer id, int priority, Consumer<AbstractMessage<H, T>> consumer) {
      super(id, priority, item -> item, consumer);
    }

    public static <H extends Header, T> MessageListenerItem<H, T> of(
        Integer id, int priority, Consumer<AbstractMessage<H, T>> consumer) {
      return new MessageListenerItem<>(id, priority, consumer);
    }
  }

  public static class MessageFutureItem<H extends Header, T>
      extends FutureItem<Integer, AbstractMessage<H, T>, AbstractMessage<H, T>> {
    public MessageFutureItem(
        Integer id, int priority, CompletableFuture<AbstractMessage<H, T>> future) {
      super(id, priority, item -> item, future);
    }

    public static <H extends Header, T> MessageFutureItem<H, T> of(Integer id, int priority) {
      return new MessageFutureItem<>(id, priority, new CompletableFuture<>());
    }
  }
}
