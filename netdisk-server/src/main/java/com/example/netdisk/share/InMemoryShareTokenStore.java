package com.example.netdisk.share;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryShareTokenStore implements ShareTokenStore {
  private static final class Val {
    final String shareId;
    final int permission;
    final long expireAtEpochSec;

    Val(String shareId, int permission, long expireAtEpochSec) {
      this.shareId = shareId;
      this.permission = permission;
      this.expireAtEpochSec = expireAtEpochSec;
    }
  }

  private final Map<String, Val> map = new ConcurrentHashMap<>();

  @Override
  public void put(String token, String shareId, int permission, long ttlSeconds) {
    long exp = Instant.now().getEpochSecond() + ttlSeconds;
    map.put(token, new Val(shareId, permission, exp));
  }

  @Override
  public ShareToken get(String token) {
    Val v = map.get(token);
    if (v == null) return null;
    if (Instant.now().getEpochSecond() > v.expireAtEpochSec) {
      map.remove(token);
      return null;
    }
    return new ShareToken(v.shareId, v.permission);
  }

  @Override
  public void delete(String token) {
    map.remove(token);
  }
}

