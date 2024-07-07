package com.javasteam.handlers;

import java.util.function.Consumer;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents a listener item. Used to store listeners in a priority queue.
 *
 * @param <I> Type of id on messages
 * @param <R> Type of raw response
 * @param <T> Type of response
 */
@Getter
@AllArgsConstructor
public class ListenerItem<I, R, T> {
  private I id;
  private int priority;
  private Function<R, T> mapper;
  private Consumer<T> consumer;
}
