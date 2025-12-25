package com.example.netdisk.auth;

import com.example.netdisk.common.ApiResponse;
import com.example.netdisk.common.BizException;
import com.example.netdisk.dao.UserQuotaRepository;
import com.example.netdisk.dao.UserRepository;
import com.example.netdisk.domain.entity.User;
import com.example.netdisk.domain.entity.UserQuota;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
  private final UserRepository userRepository;
  private final UserQuotaRepository userQuotaRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  private static final long DEFAULT_QUOTA_BYTES = 10L * 1024 * 1024 * 1024; // 10GB

  @PostMapping("/register")
  public ApiResponse<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
    if (userRepository.existsByEmail(req.getEmail())) {
      throw BizException.conflict("邮箱已注册");
    }
    User u = new User();
    u.setEmail(req.getEmail());
    u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
    userRepository.save(u);

    UserQuota q = new UserQuota();
    q.setUserId(u.getId());
    q.setQuotaBytes(DEFAULT_QUOTA_BYTES);
    q.setUsedBytes(0L);
    userQuotaRepository.save(q);

    return ApiResponse.ok(Map.of("user_id", u.getId()));
  }

  @PostMapping("/login")
  public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
    User u =
        userRepository
            .findByEmail(req.getEmail())
            .orElseThrow(() -> BizException.unauthorized("账号或密码错误"));
    if (!passwordEncoder.matches(req.getPassword(), u.getPasswordHash())) {
      throw BizException.unauthorized("账号或密码错误");
    }
    if (u.getStatus() != null && u.getStatus() != 1) {
      throw BizException.forbidden("账号被禁用");
    }
    String token = jwtService.createAccessToken(u.getId(), u.getEmail());
    return ApiResponse.ok(Map.of("access_token", token, "token_type", "Bearer", "expire_in", 7200));
  }

  @PostMapping("/logout")
  public ApiResponse<Object> logout(Authentication authentication) {
    // MVP：纯 JWT 无服务端状态，logout 仅用于前端清 token。
    // 若要强制失效：把 token jti 放入 Redis 黑名单。
    if (authentication == null) {
      throw BizException.unauthorized("未登录");
    }
    return ApiResponse.ok();
  }

  @Data
  public static class RegisterRequest {
    @NotBlank @Email private String email;
    @NotBlank private String password;
  }

  @Data
  public static class LoginRequest {
    @NotBlank @Email private String email;
    @NotBlank private String password;
  }
}

