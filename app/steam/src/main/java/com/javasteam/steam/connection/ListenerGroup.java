package com.javasteam.steam.connection;

import com.javasteam.models.AbstractMessage;
import com.javasteam.models.Header;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/**
 * A group of listeners that can be added to and triggered by a message. This class is used to
 * manage the listeners for a connection.
 */
@Slf4j
public class ListenerGroup {
  private final Map<Integer, List<Consumer<AbstractMessage<? extends Header, Object>>>>
      messageListeners;
  private final Map<Integer, List<Consumer<AbstractMessage<? extends Header, Object>>>>
      messageFutures;
  private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);

  public ListenerGroup() {
    this.messageListeners = new HashMap<>();
    this.messageFutures = new HashMap<>();
  }

  public void onMessage(int emsg, AbstractMessage<? extends Header, Object> msg) {
    messageListeners
        .getOrDefault(emsg, List.of())
        .forEach(listener -> executor.execute(() -> listener.accept(msg)));
    var futureIterator = messageFutures.getOrDefault(emsg, List.of()).listIterator();

    while (futureIterator.hasNext()) {
      var listener = futureIterator.next();
      executor.execute(() -> listener.accept(msg));
      futureIterator.remove();
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public <H extends Header, T> void addMessageListener(
      int emsg, Consumer<AbstractMessage<H, T>> listener) {
    messageListeners
        .computeIfAbsent(emsg, k -> new ArrayList<>())
        .add((Consumer) exceptionGuard(listener));
  }

  @SuppressWarnings({"unchecked"})
  public <H extends Header, T> void waitForMessage(
      int emsg, Consumer<AbstractMessage<H, T>> listener) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    messageFutures
        .computeIfAbsent(emsg, k -> new ArrayList<>())
        .add(
            msg -> {
              exceptionGuard(listener).accept((AbstractMessage<H, T>) msg);
              future.complete(null);
            });

    future.join();
  }

  public void waitForMessage(int emsg) {
    waitForMessage(emsg, msg -> {});
  }

  public <H extends Header> void notifyMessageListeners(AbstractMessage<H, Object> message) {
    onMessage(message.getEmsg(), message);
  }

  private <T> Consumer<T> exceptionGuard(Consumer<T> consumer) {
    return t -> {
      try {
        consumer.accept(t);
      } catch (Exception e) {
        log.error("Error in listener", e);
      }
    };
  }
}
