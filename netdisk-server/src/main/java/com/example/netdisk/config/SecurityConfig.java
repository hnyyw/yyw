package com.example.netdisk.config;

import com.example.netdisk.auth.JwtAuthFilter;
import com.example.netdisk.common.ApiResponse;
import com.example.netdisk.common.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
  private final JwtAuthFilter jwtAuthFilter;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable());
    http.cors(Customizer.withDefaults());
    http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    http.exceptionHandling(
        e ->
            e.authenticationEntryPoint(
                    (req, resp, ex) -> {
                      resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                      resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
                      resp.getWriter()
                          .write(
                              objectMapper.writeValueAsString(
                                  ApiResponse.error(ErrorCode.UNAUTHORIZED, "未登录或 token 失效")));
                    })
                .accessDeniedHandler(
                    (req, resp, ex) -> {
                      resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                      resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
                      resp.getWriter()
                          .write(
                              objectMapper.writeValueAsString(ApiResponse.error(ErrorCode.FORBIDDEN, "无权限")));
                    }));

    http.authorizeHttpRequests(
        auth ->
            auth.requestMatchers("/api/v1/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                .permitAll()
                .requestMatchers("/api/v1/shares/*/verify", "/api/v1/shares/*/list", "/api/v1/shares/*/download-link", "/api/v1/shares/*/save")
                .permitAll()
                .anyRequest()
                .authenticated());

    http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}

