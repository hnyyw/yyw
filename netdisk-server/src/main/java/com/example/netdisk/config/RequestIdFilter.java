package com.example.netdisk.config;

import com.example.netdisk.common.RequestId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestIdFilter extends OncePerRequestFilter {
  public static final String HEADER = "X-Request-Id";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String rid = request.getHeader(HEADER);
    if (rid == null || rid.isBlank()) {
      rid = "req_" + UUID.randomUUID();
    }
    RequestId.set(rid);
    response.setHeader(HEADER, rid);
    try {
      filterChain.doFilter(request, response);
    } finally {
      RequestId.clear();
    }
  }
}
