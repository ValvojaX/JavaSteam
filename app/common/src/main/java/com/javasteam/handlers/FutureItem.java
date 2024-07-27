package com.javasteam.handlers;

import com.google.protobuf.GeneratedMessage;
import com.javasteam.utils.proto.ProtoUtils;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import lombok.Builder;
import lombok.Getter;

/**
 * Represents a future item. Used to store futures in a priority queue.
 *
 * @param <I> Type of id on messages
 * @param <R> Type of raw response
 * @param <T> Type of response
 */
@Getter
@Builder(setterPrefix = "with")
public class FutureItem<I, R, T> {
  public static final int DEFAULT_PRIORITY = 5000;
  private I id;
  private int priority;
  private Function<R, T> mapper;
  private CompletableFuture<T> future;
  private Long timeoutMs;

  public static <I, T> FutureItemBuilder<I, T, T> builder(I id) {
    return new FutureItemBuilder<I, T, T>()
        .withId(id)
        .withMapper(o -> o)
        .withPriority(DEFAULT_PRIORITY)
        .withFuture(new CompletableFuture<>());
  }

  public static <I, R, T> FutureItemBuilder<I, R, T> builder(I id, Function<R, T> mapper) {
    return new FutureItemBuilder<I, R, T>()
        .withId(id)
        .withPriority(DEFAULT_PRIORITY)
        .withFuture(new CompletableFuture<>())
        .withMapper(mapper);
  }

  public static <I, T extends GeneratedMessage> FutureItemBuilder<I, byte[], T> builder(
      I id, Class<T> tClass) {
    return new FutureItemBuilder<I, byte[], T>()
        .withId(id)
        .withPriority(DEFAULT_PRIORITY)
        .withFuture(new CompletableFuture<>())
        .withMapper(bytes -> ProtoUtils.parseFromBytes(bytes, tClass));
  }
}
