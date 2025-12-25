package com.example.netdisk.drive;

import com.example.netdisk.auth.AuthUtil;
import com.example.netdisk.common.ApiResponse;
import com.example.netdisk.dao.EntryRepository;
import com.example.netdisk.domain.entity.Entry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DriveController {
  private final DriveService driveService;
  private final EntryRepository entryRepository;

  @PostMapping("/files/folders")
  public ApiResponse<Map<String, Object>> createFolder(@Valid @RequestBody CreateFolderRequest req) {
    long userId = AuthUtil.requirePrincipal().userId();
    long id = driveService.mkdir(userId, req.getParentId(), req.getName());
    return ApiResponse.ok(Map.of("entry_id", id));
  }

  @GetMapping("/files/list")
  public ApiResponse<Map<String, Object>> list(
      @RequestParam(name = "parent_id", required = false) Long parentId,
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "50") int pageSize,
      @RequestParam(name = "sort", defaultValue = "updated_at") String sort,
      @RequestParam(name = "order", defaultValue = "desc") String order,
      @RequestParam(name = "include_deleted", defaultValue = "0") int includeDeleted) {
    long userId = AuthUtil.requirePrincipal().userId();
    Sort.Direction dir = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
    String sortField =
        switch (sort) {
          case "name" -> "name";
          case "size_bytes" -> "sizeBytes";
          default -> "updatedAt";
        };
    Pageable pageable = PageRequest.of(Math.max(page - 1, 0), pageSize, Sort.by(dir, sortField));
    Integer isDeleted = includeDeleted == 1 ? 1 : 0;
    Long pid = (parentId == null || parentId == 0) ? null : parentId;
    var p = entryRepository.findByUserIdAndParentIdAndIsDeleted(userId, pid, isDeleted, pageable);
    return ApiResponse.ok(
        Map.of(
            "page",
            page,
            "page_size",
            pageSize,
            "total",
            p.getTotalElements(),
            "items",
            p.getContent().stream().map(this::toVo).toList()));
  }

  @PostMapping("/files/{entryId}/rename")
  public ApiResponse<Object> rename(@PathVariable("entryId") long entryId, @Valid @RequestBody RenameRequest req) {
    long userId = AuthUtil.requirePrincipal().userId();
    driveService.rename(userId, entryId, req.getName());
    return ApiResponse.ok();
  }

  @PostMapping("/files/move")
  public ApiResponse<Object> move(@Valid @RequestBody MoveRequest req) {
    long userId = AuthUtil.requirePrincipal().userId();
    driveService.move(userId, req.getEntryIds(), req.getTargetParentId());
    return ApiResponse.ok();
  }

  @PostMapping("/files/delete")
  public ApiResponse<Object> delete(@Valid @RequestBody DeleteRequest req) {
    long userId = AuthUtil.requirePrincipal().userId();
    driveService.deleteToTrash(userId, req.getEntryIds());
    return ApiResponse.ok();
  }

  @GetMapping("/trash/list")
  public ApiResponse<Map<String, Object>> trashList(
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "50") int pageSize) {
    long userId = AuthUtil.requirePrincipal().userId();
    Pageable pageable = PageRequest.of(Math.max(page - 1, 0), pageSize, Sort.by(Sort.Direction.DESC, "deletedAt"));
    var p = entryRepository.findByUserIdAndIsDeleted(userId, 1, pageable);
    return ApiResponse.ok(
        Map.of(
            "page",
            page,
            "page_size",
            pageSize,
            "total",
            p.getTotalElements(),
            "items",
            p.getContent().stream().map(this::toVo).toList()));
  }

  @PostMapping("/trash/restore")
  public ApiResponse<Object> trashRestore(@Valid @RequestBody RestoreRequest req) {
    long userId = AuthUtil.requirePrincipal().userId();
    driveService.restoreFromTrash(userId, req.getEntryIds(), req.getStrategy());
    return ApiResponse.ok();
  }

  // purge 在后面上传/引用计数一起实现

  private Map<String, Object> toVo(Entry e) {
    String type = (e.getType() != null && e.getType() == 1) ? "folder" : "file";
    DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    return Map.of(
        "id",
        e.getId(),
        "parent_id",
        e.getParentId(),
        "type",
        type,
        "name",
        e.getName(),
        "size_bytes",
        e.getSizeBytes(),
        "is_deleted",
        e.getIsDeleted() != null && e.getIsDeleted() == 1,
        "deleted_at",
        e.getDeletedAt() == null ? null : fmt.format(e.getDeletedAt()),
        "created_at",
        e.getCreatedAt() == null ? null : fmt.format(e.getCreatedAt()),
        "updated_at",
        e.getUpdatedAt() == null ? null : fmt.format(e.getUpdatedAt()));
  }

  @Data
  public static class CreateFolderRequest {
    @NotNull private Long parentId;
    @NotBlank private String name;
  }

  @Data
  public static class RenameRequest {
    @NotBlank private String name;
  }

  @Data
  public static class MoveRequest {
    @NotNull private List<Long> entryIds;
    @NotNull private Long targetParentId;
  }

  @Data
  public static class DeleteRequest {
    @NotNull private List<Long> entryIds;
  }

  @Data
  public static class RestoreRequest {
    @NotNull private List<Long> entryIds;
    @NotBlank private String strategy; // auto_rename|overwrite|fail
  }
}

