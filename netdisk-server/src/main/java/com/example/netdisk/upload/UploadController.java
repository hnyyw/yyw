package com.example.netdisk.upload;

import com.example.netdisk.auth.AuthUtil;
import com.example.netdisk.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadController {
  private final UploadService uploadService;

  @PostMapping("/precheck")
  public ApiResponse<Map<String, Object>> precheck(@Valid @RequestBody PrecheckRequest req) {
    long userId = AuthUtil.requirePrincipal().userId();
    var r = uploadService.precheck(userId, req.getParentId(), req.getName(), req.getSha256Hex(), req.getSizeBytes(), req.getConflictStrategy());
    return ApiResponse.ok(Map.of("need_upload", r.needUpload(), "entry_id", r.entryId(), "part_size", r.partSize()));
  }

  @PostMapping("/init")
  public ApiResponse<Map<String, Object>> init(@Valid @RequestBody InitUploadRequest req) {
    long userId = AuthUtil.requirePrincipal().userId();
    var r = uploadService.init(userId, req.getParentId(), req.getName(), req.getSha256Hex(), req.getSizeBytes(), req.getPartSize(), req.getConflictStrategy());
    if (!r.needUpload()) {
      return ApiResponse.ok(Map.of("need_upload", false, "entry_id", r.entryId()));
    }
    return ApiResponse.ok(
        Map.of(
            "need_upload",
            true,
            "upload_id",
            r.uploadId(),
            "part_size",
            r.partSize(),
            "total_parts",
            r.totalParts(),
            "expire_at",
            Instant.now().plusSeconds(900).toString(),
            "parts",
            r.parts().stream().map(p -> Map.of("part_number", p.partNumber(), "url", p.url())).toList()));
  }

  @GetMapping("/{uploadId}")
  public ApiResponse<Map<String, Object>> get(@PathVariable("uploadId") String uploadId) {
    long userId = AuthUtil.requirePrincipal().userId();
    var s = uploadService.getSession(userId, uploadId);
    return ApiResponse.ok(
        Map.of(
            "upload_id",
            s.uploadId(),
            "target_parent_id",
            s.targetParentId(),
            "target_name",
            s.targetName(),
            "sha256_hex",
            s.sha256Hex(),
            "size_bytes",
            s.sizeBytes(),
            "part_size",
            s.partSize(),
            "total_parts",
            s.totalParts(),
            "status",
            s.status(),
            "uploaded_parts",
            s.uploadedParts().stream().map(p -> Map.of("part_number", p.partNumber(), "etag", p.etag())).toList()));
  }

  @PostMapping("/complete")
  public ApiResponse<Map<String, Object>> complete(@Valid @RequestBody CompleteUploadRequest req) {
    long userId = AuthUtil.requirePrincipal().userId();
    var parts = req.getParts().stream().map(p -> new UploadService.UploadedPart(p.getPartNumber(), p.getEtag())).toList();
    var r = uploadService.complete(userId, req.getUploadId(), parts, req.getSha256Hex());
    return ApiResponse.ok(Map.of("entry_id", r.entryId(), "blob_id", r.blobId()));
  }

  @PostMapping("/{uploadId}/abort")
  public ApiResponse<Object> abort(@PathVariable("uploadId") String uploadId) {
    long userId = AuthUtil.requirePrincipal().userId();
    uploadService.abort(userId, uploadId);
    return ApiResponse.ok();
  }

  @Data
  public static class PrecheckRequest {
    @NotNull private Long parentId;
    @NotBlank private String name;
    @NotBlank private String sha256Hex;
    @NotNull private Long sizeBytes;
    private String conflictStrategy = "fail";
  }

  @Data
  public static class InitUploadRequest {
    @NotNull private Long parentId;
    @NotBlank private String name;
    @NotBlank private String sha256Hex;
    @NotNull private Long sizeBytes;
    private Integer partSize;
    private String conflictStrategy = "fail";
  }

  @Data
  public static class CompleteUploadRequest {
    @NotBlank private String uploadId;
    @NotBlank private String sha256Hex;
    @NotNull private List<Part> parts;
  }

  @Data
  public static class Part {
    @NotNull private Integer partNumber;
    @NotBlank private String etag;
  }
}

