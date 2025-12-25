package com.example.netdisk.auth;

import com.example.netdisk.common.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthUtil {
  private AuthUtil() {}

  public static JwtPrincipal requirePrincipal() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof JwtPrincipal p)) {
      throw BizException.unauthorized("未登录");
    }
    return p;
  }
}

