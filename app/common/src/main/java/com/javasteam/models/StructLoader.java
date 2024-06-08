package com.javasteam.models;

import java.util.function.Function;

/**
 * StructLoader is an interface that provides a loader for a struct. It is used to load the struct
 * from a byte array.
 *
 * @param <T> the type of the struct
 */
public interface StructLoader<T> {
  /** Returns the loader function that loads the struct from a byte array. */
  Function<byte[], T> getLoader();

  /** Returns the EMsg value that is mapped to the struct. */
  int getEmsg();
}
