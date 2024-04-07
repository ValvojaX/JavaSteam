package com.javasteam.utils.proto;

import static com.javasteam.protobufs.EnumsClientserver.EMsg;

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

  public static Optional<EMsg> resolveEMsg(int emsg) {
    return Arrays.stream(EMsg.values()).filter(value -> value.getNumber() == emsg).findFirst();
  }
}
