package com.example.netdisk.common;

public final class RequestId {
  private static final ThreadLocal<String> TL = new ThreadLocal<>();

  private RequestId() {}

  public static String get() {
    String v = TL.get();
    return v == null ? "" : v;
  }

  public static void set(String requestId) {
    TL.set(requestId);
  }

  public static void clear() {
    TL.remove();
  }
}
