package com.javasteam.handlers;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents a future item. Used to store futures in a priority queue.
 *
 * @param <I> Type of id on messages
 * @param <R> Type of raw response
 * @param <T> Type of response
 */
@Getter
@AllArgsConstructor
public class FutureItem<I, R, T> {
  protected I id;
  private int priority;
  private Function<R, T> mapper;
  private CompletableFuture<T> future;
}
