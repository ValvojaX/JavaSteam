package com.javasteam.utils.proto;

import static com.javasteam.protobufs.EnumsClientserver.EMsg;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Optional;

/** Utility class for common proto operations. */
public class ProtoUtils {
  private static final int PROTO_MASK = 0x80000000;

  public static boolean isProto(int value) {
    return (value & PROTO_MASK) != 0;
  }

  public static int clearProtoMask(int value) {
    return value & ~PROTO_MASK;
  }

  public static int setProtoMask(int value) {
    return value | PROTO_MASK;
  }

  @SuppressWarnings("unchecked")
  public static <T extends GeneratedMessage> T parseFromBytes(byte[] bytes, Class<T> tClass) {
    try {
      T instance = (T) tClass.getMethod("getDefaultInstance").invoke(null);
      return (T) instance.toBuilder().mergeFrom(bytes).build();
    } catch (IllegalAccessException
        | InvocationTargetException
        | NoSuchMethodException
        | InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }
  }

  public static Optional<EMsg> resolveEMsg(int emsg) {
    return Arrays.stream(EMsg.values()).filter(value -> value.getNumber() == emsg).findFirst();
  }
}
