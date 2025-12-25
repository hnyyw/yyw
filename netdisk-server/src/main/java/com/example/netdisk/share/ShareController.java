package com.example.netdisk.share;

import com.example.netdisk.auth.AuthUtil;
import com.example.netdisk.common.ApiResponse;
import com.example.netdisk.config.NetdiskProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shares")
@RequiredArgsConstructor
public class ShareController {
  private final ShareService shareService;
  private final NetdiskProperties props;

  @PostMapping
  public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CreateShareRequest req) {
    long userId = AuthUtil.requirePrincipal().userId();
    LocalDateTime expiredAt = req.getExpiredAt() == null || req.getExpiredAt().isBlank() ? null : LocalDateTime.parse(req.getExpiredAt());
    var s = shareService.create(userId, req.getRootEntryId(), req.getPermission(), req.getCode(), expiredAt);
    String shareUrl = "http://localhost:5173/share/" + s.getShareId(); // 前端路由示例
    return ApiResponse.ok(Map.of("share_id", s.getShareId(), "share_url", shareUrl));
  }

  @PostMapping("/{shareId}/verify")
  public ApiResponse<Map<String, Object>> verify(@PathVariable("shareId") String shareId, @RequestBody(required = false) VerifyShareRequest req) {
    String code = req == null ? null : req.getCode();
    String token = shareService.verify(shareId, code);
    return ApiResponse.ok(Map.of("share_token", token, "expire_in", props.getPresign().getShareTokenTtlSeconds()));
  }

  @GetMapping("/{shareId}/list")
  public ApiResponse<Map<String, Object>> list(
      @PathVariable("shareId") String shareId,
      @RequestHeader("X-Share-Token") String shareToken,
      @RequestParam(name = "parent_entry_id", required = false) Long parentEntryId) {
    List<Map<String, Object>> items = shareService.list(shareId, shareToken, parentEntryId);
    // root_entry_id 在 service 内可取，但这里简化不返回
    return ApiResponse.ok(Map.of("share_id", shareId, "items", items));
  }

  @PostMapping("/{shareId}/download-link")
  public ApiResponse<Map<String, Object>> downloadLink(
      @PathVariable("shareId") String shareId,
      @RequestHeader("X-Share-Token") String shareToken,
      @Valid @RequestBody ShareDownloadLinkRequest req) {
    String url = shareService.shareDownloadLink(shareId, shareToken, req.getEntryId());
    return ApiResponse.ok(Map.of("url", url, "expire_in", props.getPresign().getDownloadExpireSeconds()));
  }

  @PostMapping("/{shareId}/save")
  public ApiResponse<Object> save(
      @PathVariable("shareId") String shareId,
      @RequestHeader("X-Share-Token") String shareToken,
      @Valid @RequestBody ShareSaveRequest req) {
    long userId = AuthUtil.requirePrincipal().userId();
    shareService.saveToMyDrive(shareId, shareToken, req.getEntryId(), userId, req.getTargetParentId());
    return ApiResponse.ok();
  }

  @PostMapping("/{shareId}/cancel")
  public ApiResponse<Object> cancel(@PathVariable("shareId") String shareId) {
    long userId = AuthUtil.requirePrincipal().userId();
    shareService.cancel(userId, shareId);
    return ApiResponse.ok();
  }

  @Data
  public static class CreateShareRequest {
    @NotNull private Long rootEntryId;
    @NotBlank private String permission; // preview|download|save
    private String code;
    private String expiredAt; // ISO-LOCAL-DATE-TIME
  }

  @Data
  public static class VerifyShareRequest {
    private String code;
  }

  @Data
  public static class ShareDownloadLinkRequest {
    @NotNull private Long entryId;
  }

  @Data
  public static class ShareSaveRequest {
    @NotNull private Long entryId;
    @NotNull private Long targetParentId;
  }
}

