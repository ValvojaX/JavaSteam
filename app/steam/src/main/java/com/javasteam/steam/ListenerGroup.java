package com.javasteam.steam;

import com.javasteam.models.AbstractMessage;
import com.javasteam.models.HasReadWriteLock;
import com.javasteam.models.Header;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * A group of listeners that can be added to and triggered by a message. This class is used to
 * manage the listeners for a connection.
 */
@Slf4j
public class ListenerGroup implements HasReadWriteLock {
  private static final int DEFAULT_THREADS = 10;
  private final List<ListenerGroupItem> items;
  private final ScheduledExecutorService executor;
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

  public ListenerGroup() {
    this.executor = Executors.newScheduledThreadPool(DEFAULT_THREADS);
    this.items = new ArrayList<>();
  }

  public ListenerGroup(int threads) {
    this.executor = Executors.newScheduledThreadPool(threads);
    this.items = new ArrayList<>();
  }

  public void onMessage(int emsg, AbstractMessage<? extends Header, Object> msg) {
    getItems(item -> item.getEmsg() == emsg)
        .forEach(
            item ->
                item.getConsumer()
                    .ifPresent(
                        consumer -> {
                          executor.execute(() -> exceptionGuard(consumer).accept(msg));
                          item.getFuture().map(f -> f.complete(null));
                        }));
  }

  public ListenerGroupItem addMessageListener(ListenerGroupItem item) {
    withWriteLock(() -> items.add(item));
    return item;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public <H extends Header, T> ListenerGroupItem addMessageListener(
      int emsg, Consumer<AbstractMessage<H, T>> listener) {
    var item = ListenerGroupItem.builder().setEmsg(emsg).setConsumer((Consumer) listener).build();
    addMessageListener(item);
    return item;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public <H extends Header, T> void waitForMessage(
      int emsg, Consumer<AbstractMessage<H, T>> listener) {
    var future = new CompletableFuture<Void>();
    var item =
        addMessageListener(
            ListenerGroupItem.builder()
                .setEmsg(emsg)
                .setConsumer((Consumer) listener)
                .setFuture(future)
                .build());

    future.join();
    removeItem(item);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public <H extends Header, T> void waitForMessage(
      int emsg, Consumer<AbstractMessage<H, T>> listener, long timeoutMs) throws TimeoutException {
    var future = new CompletableFuture<Void>();
    var item =
        addMessageListener(
            ListenerGroupItem.builder()
                .setEmsg(emsg)
                .setConsumer((Consumer) listener)
                .setFuture(future)
                .setTimeout(timeoutMs)
                .build());

    try {
      future.get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    } catch (InterruptedException | ExecutionException exception) {
      log.error("Error waiting for message", exception);
    } catch (TimeoutException exception) {
      removeItem(item);
      log.warn("Timeout waiting for message {}", emsg);
      throw exception;
    }
  }

  public void waitForMessage(int emsg) {
    waitForMessage(emsg, msg -> {});
  }

  public void waitForMessage(int emsg, long timeoutMs) throws TimeoutException {
    waitForMessage(emsg, msg -> {}, timeoutMs);
  }

  public <H extends Header> void notifyMessageListeners(AbstractMessage<H, Object> message) {
    onMessage(message.getEMsg(), message);
  }

  private List<ListenerGroupItem> getItems(Predicate<ListenerGroupItem> predicate) {
    return withReadLock(() -> items.stream().filter(predicate).toList());
  }

  private void removeItem(ListenerGroupItem item) {
    withWriteLock(() -> items.remove(item));
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

  @Override
  public ReentrantReadWriteLock getLock() {
    return lock;
  }

  @Getter
  @Builder(setterPrefix = "set")
  public static class ListenerGroupItem {
    private int emsg;
    private Consumer<AbstractMessage<? extends Header, Object>> consumer;
    private CompletableFuture<Void> future;
    private Long timeout;

    public Optional<Consumer<AbstractMessage<? extends Header, Object>>> getConsumer() {
      return Optional.ofNullable(consumer);
    }

    public Optional<CompletableFuture<Void>> getFuture() {
      return Optional.ofNullable(future);
    }
  }
}
