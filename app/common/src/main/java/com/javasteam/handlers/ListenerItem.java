package com.javasteam.handlers;

import com.google.protobuf.GeneratedMessage;
import com.javasteam.utils.proto.ProtoUtils;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Represents a listener item. Used to store listeners in a priority queue.
 *
 * @param <I> Type of id on messages
 * @param <R> Type of raw response
 * @param <T> Type of response
 */
@Getter
@Builder(setterPrefix = "with")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ListenerItem<I, R, T> {
  public static final int DEFAULT_PRIORITY = 5000;
  private I id;
  private int priority;
  private Function<R, T> mapper;
  private Consumer<T> consumer;

  public static <I, R, T> ListenerItemBuilder<I, R, T> builder(
      I id, Function<R, T> mapper, Consumer<T> consumer) {
    return new ListenerItemBuilder<I, R, T>()
        .withId(id)
        .withPriority(DEFAULT_PRIORITY)
        .withMapper(mapper)
        .withConsumer(consumer);
  }

  public static <I, T extends GeneratedMessage> ListenerItemBuilder<I, byte[], T> builder(
      I id, Class<T> tClass, Consumer<T> consumer) {
    return new ListenerItemBuilder<I, byte[], T>()
        .withId(id)
        .withPriority(DEFAULT_PRIORITY)
        .withMapper(bytes -> ProtoUtils.parseFromBytes(bytes, tClass))
        .withConsumer(consumer);
  }

  public static <I, T> ListenerItemBuilder<I, T, T> builder(I id, Consumer<T> consumer) {
    return new ListenerItemBuilder<I, T, T>()
        .withId(id)
        .withPriority(DEFAULT_PRIORITY)
        .withMapper(o -> o)
        .withConsumer(consumer);
  }
}
