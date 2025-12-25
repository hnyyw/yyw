package com.example.netdisk.upload;

public final class ObjectKeyUtil {
  private ObjectKeyUtil() {}

  public static String blobKeyFromSha256Hex(String sha256Hex) {
    String h = sha256Hex.toLowerCase();
    String aa = h.substring(0, 2);
    String bb = h.substring(2, 4);
    return "blob/sha256/" + aa + "/" + bb + "/" + h;
  }
}

