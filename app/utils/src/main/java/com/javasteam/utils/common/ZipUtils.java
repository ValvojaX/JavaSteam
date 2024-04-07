package com.javasteam.utils.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Utility class for common zip operations. */
public class ZipUtils {
  public static byte[] unzip(byte[] data) {
    try {
      ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
      GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024];
      int length;
      while ((length = gzipInputStream.read(buffer)) > 0) {
        byteArrayOutputStream.write(buffer, 0, length);
      }
      gzipInputStream.close();
      byteArrayOutputStream.close();
      return byteArrayOutputStream.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("Failed to unzip data", e);
    }
  }

  public static byte[] zip(byte[] data) {
    try {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
      gzipOutputStream.write(data);
      gzipOutputStream.close();
      return byteArrayOutputStream.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("Failed to zip data", e);
    }
  }
}
