package com.javasteam.steam.connection;

import com.javasteam.models.steam.BaseMsg;
import com.javasteam.models.steam.BaseMsgHeader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/**
 * A group of listeners that can be added to and triggered by a message. This class is used to
 * manage the listeners for a connection.
 */
@Slf4j
public class ListenerGroup {
  private final Map<Integer, List<Consumer<BaseMsg<? extends BaseMsgHeader, Object>>>>
      messageListeners;
  private final Map<Integer, List<CompletableFuture<Void>>> messageFutures;
  private final ExecutorService executor = Executors.newFixedThreadPool(10);

  public ListenerGroup() {
    this.messageListeners = new HashMap<>();
    this.messageFutures = new HashMap<>();
  }

  public void onMessage(int emsg, BaseMsg<? extends BaseMsgHeader, Object> msg) {
    executor.execute(
        () -> {
          messageListeners.getOrDefault(emsg, List.of()).forEach(listener -> listener.accept(msg));
          messageFutures.getOrDefault(emsg, List.of()).forEach(future -> future.complete(null));

          Optional.ofNullable(messageFutures.get(emsg))
              .ifPresent(futures -> futures.removeIf(CompletableFuture::isDone));
        });
  }

  @SuppressWarnings("unchecked")
  public <H extends BaseMsgHeader, T> void addMessageListener(
      int emsg, Consumer<BaseMsg<H, T>> listener) {
    messageListeners
        .computeIfAbsent(emsg, k -> new ArrayList<>())
        .add((Consumer) exceptionGuard(listener));
  }

  public void waitForMessage(int emsg) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    messageFutures.computeIfAbsent(emsg, k -> new ArrayList<>()).add(future);
    future.join();
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
