package com.javasteam.steam.handlers;

import com.javasteam.models.HasReadWriteLock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/** A class for handling incoming and outgoing jobs. */
@Slf4j
public class JobHandler implements HasReadWriteLock {
  private static final int DEFAULT_THREADS = 10;
  private final List<JobItem<?>> items;
  private final ScheduledExecutorService executor;
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private int jobIdCounter = 0;

  public JobHandler() {
    this.executor = Executors.newScheduledThreadPool(DEFAULT_THREADS);
    this.items = new ArrayList<>();
  }

  public JobHandler(int threads) {
    this.executor = Executors.newScheduledThreadPool(threads);
    this.items = new ArrayList<>();
  }

  public synchronized long getNextJobId() {
    return ++jobIdCounter;
  }

  public void onJob(long targetJobId, byte[] bodyBytes) {
    getItems(item -> item.getSourceJobId() == targetJobId)
        .forEach(
            item ->
                item.getConsumer()
                    .ifPresent(
                        consumer -> {
                          var response = item.getResponseSupplier().apply(bodyBytes);
                          executor.execute(() -> exceptionGuard(consumer).accept(response));
                          item.getFuture().map(f -> f.complete(null));
                        }));
  }

  public <T> JobItem<T> addJobListener(JobItem<T> jobItem) {
    withWriteLock(() -> items.add(jobItem));
    return jobItem;
  }

  public <T> JobItem<T> addJobListener(
      long sourceJobId, Function<byte[], T> responseSupplier, Consumer<T> listener) {
    return addJobListener(
        JobItem.<T>builder()
            .setSourceJobId(sourceJobId)
            .setResponseSupplier(responseSupplier)
            .setConsumer(listener)
            .build());
  }

  public <T> void waitForJob(
      long sourceJobId, Function<byte[], T> responseSupplier, Consumer<T> listener) {
    var future = new CompletableFuture<Void>();
    var item =
        addJobListener(
            JobItem.<T>builder()
                .setSourceJobId(sourceJobId)
                .setResponseSupplier(responseSupplier)
                .setConsumer(listener)
                .setFuture(future)
                .build());

    future.join();
    removeItem(item);
  }

  public <T> void waitForJob(
      long sourceJobId, Function<byte[], T> responseSupplier, Consumer<T> listener, long timeoutMs)
      throws TimeoutException {
    var future = new CompletableFuture<Void>();
    var item =
        addJobListener(
            JobItem.<T>builder()
                .setSourceJobId(sourceJobId)
                .setResponseSupplier(responseSupplier)
                .setConsumer(listener)
                .setFuture(future)
                .setTimeout(timeoutMs)
                .build());

    try {
      future.get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    } catch (InterruptedException | ExecutionException exception) {
      throw new TimeoutException("Timeout waiting for job");
    } catch (TimeoutException exception) {
      removeItem(item);
      log.warn("Timeout waiting for job {}", sourceJobId);
      throw exception;
    }
  }

  public void waitForJob(long sourceJobId) {
    waitForJob(sourceJobId, bytes -> null, response -> {});
  }

  public void waitForJob(long sourceJobId, long timeoutMs) throws TimeoutException {
    waitForJob(sourceJobId, bytes -> null, response -> {}, timeoutMs);
  }

  public void notifyJobListeners(long sourceJobId, byte[] bodyBytes) {
    onJob(sourceJobId, bodyBytes);
  }

  @SuppressWarnings("unchecked")
  private List<JobItem<Object>> getItems(Predicate<JobItem<Object>> predicate) {
    return withReadLock(
        () -> items.stream().map(item -> (JobItem<Object>) item).filter(predicate).toList());
  }

  private void removeItem(JobItem<?> item) {
    withWriteLock(() -> items.remove(item));
  }

  private <T> Consumer<T> exceptionGuard(Consumer<T> consumer) {
    return t -> {
      try {
        consumer.accept(t);
      } catch (Exception e) {
        log.error("Error in job handler", e);
      }
    };
  }

  @Override
  public ReentrantReadWriteLock getLock() {
    return lock;
  }

  @Getter
  @Builder(setterPrefix = "set")
  public static class JobItem<T> {
    private long sourceJobId;
    private Function<byte[], T> responseSupplier;
    private Consumer<T> consumer;
    private CompletableFuture<Void> future;
    private Long timeout;

    public Optional<Consumer<T>> getConsumer() {
      return Optional.ofNullable(consumer);
    }

    public Optional<CompletableFuture<Void>> getFuture() {
      return Optional.ofNullable(future);
    }
  }
}
