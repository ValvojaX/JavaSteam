package com.javasteam.handlers;

import com.javasteam.models.HasReadWriteLock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class for threaded message handlers.
 *
 * @param <I> Type of the message id
 */
@Slf4j
public class BaseMessageHandler<I> implements HasReadWriteLock {
  private static final int DEFAULT_THREADS = 10;
  private final List<ListenerItem<I, Object, Object>> listeners;
  private final List<FutureItem<I, Object, Object>> futures;
  private final ScheduledExecutorService executor;
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

  public BaseMessageHandler() {
    this.executor = Executors.newScheduledThreadPool(DEFAULT_THREADS);
    this.listeners = new ArrayList<>();
    this.futures = new ArrayList<>();
  }

  public BaseMessageHandler(int threads) {
    this.executor = Executors.newScheduledThreadPool(threads);
    this.listeners = new ArrayList<>();
    this.futures = new ArrayList<>();
  }

  private <R> void onMessage(I id, R msg) {
    // Handle listeners
    withReadLock(() -> listeners).stream()
        .filter(item -> Objects.equals(id, item.getId()))
        .sorted((a, b) -> Integer.compare(b.getPriority(), a.getPriority()))
        .forEach(
            item -> {
              try {
                executor.execute(() -> item.getConsumer().accept(item.getMapper().apply(msg)));
              } catch (Exception e) {
                log.error("Error processing message", e);
              }
            });

    // Handle futures
    withWriteLock(() -> futures).stream()
        .filter(item -> Objects.equals(id, item.getId()))
        .sorted((a, b) -> Integer.compare(b.getPriority(), a.getPriority()))
        .forEach(
            item -> {
              try {
                item.getFuture().complete(item.getMapper().apply(msg));
                futures.remove(item);
              } catch (Exception e) {
                log.error("Error processing message", e);
              }
            });
  }

  public <R> void notifyListeners(I id, R msg) {
    onMessage(id, msg);
  }

  @SuppressWarnings("unchecked")
  public <T, R> ListenerItem<I, R, T> addMessageListener(ListenerItem<I, R, T> item) {
    withWriteLock(() -> listeners.add((ListenerItem<I, Object, Object>) item));
    return item;
  }

  @SuppressWarnings("unchecked")
  public <T, R> T addMessageFuture(FutureItem<I, R, T> item) {
    try {
      withWriteLock(() -> futures.add((FutureItem<I, Object, Object>) item));
      if (item.getTimeoutMs() != null) {
        return item.getFuture()
            .get(item.getTimeoutMs(), java.util.concurrent.TimeUnit.MILLISECONDS);
      }
      return item.getFuture().get();
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException("Timeout waiting for message %s".formatted(item.getId()));
    }
  }

  public <R> void removeListenerItem(ListenerItem<I, R, ?> item) {
    withWriteLock(() -> listeners.removeIf(i -> i.equals(item)));
  }

  @Override
  public ReentrantReadWriteLock getLock() {
    return lock;
  }
}
