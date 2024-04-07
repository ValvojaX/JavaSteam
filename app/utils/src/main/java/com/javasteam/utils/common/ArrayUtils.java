package com.javasteam.utils.common;

import java.util.Arrays;
import java.util.stream.Stream;

/** Utility class for common array operations. */
public class ArrayUtils {
  public static byte[] concat(byte[]... arrays) {
    int size = Stream.of(arrays).mapToInt(array -> array.length).sum();
    byte[] result = new byte[size];
    int offset = 0;
    for (byte[] array : arrays) {
      System.arraycopy(array, 0, result, offset, array.length);
      offset += array.length;
    }
    return result;
  }

  public static byte[] subarray(byte[] array, int start, int size) {
    return Arrays.copyOfRange(array, start, start + size);
  }

  public static byte[] subarray(byte[] array, int size) {
    return subarray(array, 0, size);
  }

  public static boolean equals(byte[] array1, byte[] array2, int start, int end) {
    if (end > array1.length || end > array2.length) {
      return false;
    }

    return Arrays.equals(
        Arrays.copyOfRange(array1, start, end), Arrays.copyOfRange(array2, start, end));
  }
}
