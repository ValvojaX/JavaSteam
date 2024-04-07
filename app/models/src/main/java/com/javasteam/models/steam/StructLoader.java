package com.javasteam.models.steam;

import java.util.function.Function;

/**
 * StructLoader is an interface that provides a loader for a struct. It is used to load the struct
 * from a byte array.
 *
 * @param <T> the type of the struct
 */
public interface StructLoader<T> {
  Function<byte[], T> getLoader();
}
