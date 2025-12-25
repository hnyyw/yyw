package com.example.netdisk.share;

public interface ShareTokenStore {
  void put(String token, String shareId, int permission, long ttlSeconds);

  ShareToken get(String token);

  void delete(String token);

  record ShareToken(String shareId, int permission) {}
}

