package com.javasteam.steam.handlers;

import com.javasteam.handlers.BaseMessageHandler;
import com.javasteam.handlers.FutureItem;
import com.javasteam.handlers.ListenerItem;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

/** A class for handling incoming and outgoing jobs. */
@Slf4j
public class JobHandler extends BaseMessageHandler<Long> {
  public static final int DEFAULT_PRIORITY = 0;
  private final AtomicInteger jobIdCounter = new AtomicInteger(0);

  public JobHandler() {
    super();
  }

  public JobHandler(int threads) {
    super(threads);
  }

  public synchronized long getNextJobId() {
    return jobIdCounter.incrementAndGet();
  }

  public static class JobListenerItem<T> extends ListenerItem<Long, byte[], T> {
    public JobListenerItem(
        Long id, int priority, Function<byte[], T> mapper, Consumer<T> consumer) {
      super(id, priority, mapper, consumer);
    }

    public static <T> JobListenerItem<T> of(
        Long id, int priority, Function<byte[], T> mapper, Consumer<T> consumer) {
      return new JobListenerItem<>(id, priority, mapper, consumer);
    }
  }

  public static class JobFutureItem<T> extends FutureItem<Long, byte[], T> {
    public JobFutureItem(
        Long id, int priority, Function<byte[], T> mapper, CompletableFuture<T> future) {
      super(id, priority, mapper, future);
    }

    public static <T> JobFutureItem<T> of(Long id, int priority, Function<byte[], T> mapper) {
      return new JobFutureItem<>(id, priority, mapper, new CompletableFuture<>());
    }
  }
}
