package com.example.netdisk.upload;

public final class Hex {
  private static final char[] HEX = "0123456789abcdef".toCharArray();

  private Hex() {}

  public static byte[] toBytes(String hex) {
    if (hex == null || hex.length() != 64) {
      throw new IllegalArgumentException("sha256_hex must be 64 hex chars");
    }
    byte[] out = new byte[32];
    for (int i = 0; i < 32; i++) {
      int hi = Character.digit(hex.charAt(i * 2), 16);
      int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
      if (hi < 0 || lo < 0) throw new IllegalArgumentException("invalid hex");
      out[i] = (byte) ((hi << 4) | lo);
    }
    return out;
  }

  public static String toHex(byte[] bytes) {
    if (bytes == null) return "";
    char[] out = new char[bytes.length * 2];
    for (int i = 0; i < bytes.length; i++) {
      int v = bytes[i] & 0xFF;
      out[i * 2] = HEX[v >>> 4];
      out[i * 2 + 1] = HEX[v & 0x0F];
    }
    return new String(out);
  }
}

