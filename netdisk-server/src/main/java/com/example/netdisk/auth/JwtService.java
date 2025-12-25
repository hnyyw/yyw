package com.example.netdisk.auth;

import com.example.netdisk.config.NetdiskProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {
  private final NetdiskProperties props;

  private SecretKey key() {
    byte[] bytes = props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(bytes);
  }

  public String createAccessToken(long userId, String email) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(props.getJwt().getAccessTokenTtlSeconds());
    return Jwts.builder()
        .issuer(props.getJwt().getIssuer())
        .issuedAt(Date.from(now))
        .expiration(Date.from(exp))
        .subject(String.valueOf(userId))
        .claim("email", email)
        .signWith(key())
        .compact();
  }

  public JwtPrincipal parse(String token) {
    Claims claims = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    long userId = Long.parseLong(claims.getSubject());
    String email = claims.get("email", String.class);
    return new JwtPrincipal(userId, email);
  }
}

