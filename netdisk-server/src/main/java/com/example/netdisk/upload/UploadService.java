package com.example.netdisk.upload;

import com.example.netdisk.common.BizException;
import com.example.netdisk.config.NetdiskProperties;
import com.example.netdisk.dao.BlobRepository;
import com.example.netdisk.dao.EntryRepository;
import com.example.netdisk.dao.UploadSessionRepository;
import com.example.netdisk.dao.UserQuotaRepository;
import com.example.netdisk.domain.entity.Blob;
import com.example.netdisk.domain.entity.Entry;
import com.example.netdisk.domain.entity.UploadSession;
import com.example.netdisk.drive.DriveService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.CompletedPart;

@Service
@RequiredArgsConstructor
public class UploadService {
  private final NetdiskProperties props;
  private final S3Facade s3;
  private final BlobRepository blobRepository;
  private final EntryRepository entryRepository;
  private final UploadSessionRepository uploadSessionRepository;
  private final UserQuotaRepository userQuotaRepository;
  private final DriveService driveService;

  public PrecheckResult precheck(long userId, Long parentId, String name, String sha256Hex, long sizeBytes, String conflictStrategy) {
    byte[] sha = Hex.toBytes(sha256Hex);
    Blob blob = blobRepository.findBySha256AndSizeBytes(sha, sizeBytes).orElse(null);
    if (blob != null && blob.getStatus() != null && blob.getStatus() == 1) {
      // 秒传：直接创建 entry 引用 blob
      return new PrecheckResult(false, createEntryByBlob(userId, parentId, name, blob, conflictStrategy), null);
    }
    int partSize = (int) props.getUpload().getPartSizeDefaultBytes();
    return new PrecheckResult(true, null, partSize);
  }

  @Transactional
  public long createEntryByBlob(long userId, Long parentId, String name, Blob blob, String conflictStrategy) {
    // 配额检查（MVP：used + size <= quota）
    var quota = userQuotaRepository.findById(userId).orElseThrow(() -> BizException.forbidden("未初始化配额"));
    if (quota.getUsedBytes() + blob.getSizeBytes() > quota.getQuotaBytes()) {
      throw BizException.forbidden("容量不足");
    }
    Long pid = (parentId == null || parentId == 0) ? null : parentId;
    String finalName = name;
    if ("auto_rename".equals(conflictStrategy)) {
      finalName = driveService.ensureUniqueName(userId, pid, name);
    } else if ("fail".equals(conflictStrategy) || conflictStrategy == null) {
      if (entryRepository.existsByUserIdAndParentIdAndNameAndIsDeleted(userId, pid, name, 0)) {
        throw BizException.conflict("同目录已存在同名项");
      }
    }
    Entry e = new Entry();
    e.setUserId(userId);
    e.setParentId(pid);
    e.setType(2);
    e.setName(finalName);
    e.setBlobId(blob.getId());
    e.setSizeBytes(blob.getSizeBytes());
    e.setIsDeleted(0);
    try {
      entryRepository.save(e);
    } catch (DataIntegrityViolationException ex) {
      throw BizException.conflict("同目录已存在同名项");
    }
    blob.setRefCount(blob.getRefCount() + 1);
    blobRepository.save(blob);
    quota.setUsedBytes(quota.getUsedBytes() + blob.getSizeBytes());
    userQuotaRepository.save(quota);
    return e.getId();
  }

  @Transactional
  public InitUploadResult init(long userId, Long parentId, String name, String sha256Hex, long sizeBytes, Integer reqPartSize, String conflictStrategy) {
    // 如果已存在 blob，直接走秒传（容错）
    byte[] sha = Hex.toBytes(sha256Hex);
    Blob blob = blobRepository.findBySha256AndSizeBytes(sha, sizeBytes).orElse(null);
    if (blob != null && blob.getStatus() != null && blob.getStatus() == 1) {
      long entryId = createEntryByBlob(userId, parentId, name, blob, conflictStrategy);
      return InitUploadResult.fast(entryId);
    }

    int partSize = normalizePartSize(reqPartSize);
    int totalParts = (int) ((sizeBytes + partSize - 1) / partSize);
    if (totalParts <= 0) totalParts = 1;

    Long pid = (parentId == null || parentId == 0) ? null : parentId;
    String finalName = name;
    if ("auto_rename".equals(conflictStrategy)) {
      finalName = driveService.ensureUniqueName(userId, pid, name);
    } else if ("fail".equals(conflictStrategy) || conflictStrategy == null) {
      if (entryRepository.existsByUserIdAndParentIdAndNameAndIsDeleted(userId, pid, name, 0)) {
        throw BizException.conflict("同目录已存在同名项");
      }
    }

    String uploadId = "u_" + UUID.randomUUID().toString().replace("-", "");
    String objectKey = ObjectKeyUtil.blobKeyFromSha256Hex(sha256Hex);
    String s3UploadId = s3.createMultipartUpload(objectKey);

    UploadSession session = new UploadSession();
    session.setUploadId(uploadId);
    session.setUserId(userId);
    session.setTargetParentId(pid == null ? 0L : pid);
    session.setTargetName(finalName);
    session.setSha256(sha);
    session.setSizeBytes(sizeBytes);
    session.setPartSize(partSize);
    session.setTotalParts(totalParts);
    session.setObjectKey(objectKey);
    session.setS3UploadId(s3UploadId);
    session.setStatus(2); // uploading
    session.setExpiredAt(LocalDateTime.now().plusMinutes(props.getUpload().getSessionTtlMinutes()));
    uploadSessionRepository.save(session);

    List<PresignedPart> parts = new ArrayList<>(totalParts);
    for (int i = 1; i <= totalParts; i++) {
      String url = s3.presignUploadPartUrl(objectKey, s3UploadId, i, props.getPresign().getUploadPartExpireSeconds());
      parts.add(new PresignedPart(i, url));
    }
    return InitUploadResult.multipart(uploadId, partSize, totalParts, parts);
  }

  public UploadSessionView getSession(long userId, String uploadId) {
    UploadSession s = uploadSessionRepository.findByUploadId(uploadId).orElseThrow(() -> BizException.notFound("upload_id 不存在"));
    if (s.getUserId() != userId) throw BizException.forbidden("无权限");
    if (s.getExpiredAt() != null && s.getExpiredAt().isBefore(LocalDateTime.now()) && s.getStatus() != 3 && s.getStatus() != 4) {
      s.setStatus(5);
      uploadSessionRepository.save(s);
    }
    List<UploadedPart> uploaded = new ArrayList<>();
    if (s.getS3UploadId() != null && s.getObjectKey() != null && (s.getStatus() == 2 || s.getStatus() == 1)) {
      var parts = s3.listUploadedParts(s.getObjectKey(), s.getS3UploadId());
      for (var p : parts) {
        uploaded.add(new UploadedPart(p.partNumber(), p.eTag()));
      }
    }
    return UploadSessionView.from(s, Hex.toHex(s.getSha256()), uploaded);
  }

  @Transactional
  public CompleteResult complete(long userId, String uploadId, List<UploadedPart> parts, String sha256Hex) {
    UploadSession s = uploadSessionRepository.findByUploadId(uploadId).orElseThrow(() -> BizException.notFound("upload_id 不存在"));
    if (s.getUserId() != userId) throw BizException.forbidden("无权限");
    if (s.getStatus() != null && s.getStatus() == 3) {
      return new CompleteResult(s.getResultEntryId(), s.getResultBlobId());
    }
    if (s.getStatus() != null && (s.getStatus() == 4 || s.getStatus() == 5)) {
      throw BizException.conflict("上传会话已终止/过期");
    }
    if (s.getExpiredAt() != null && s.getExpiredAt().isBefore(LocalDateTime.now())) {
      s.setStatus(5);
      uploadSessionRepository.save(s);
      throw BizException.conflict("上传会话已过期");
    }
    if (parts == null || parts.isEmpty()) {
      throw BizException.badRequest("parts 不能为空");
    }
    // S3 complete 需要按 partNumber 升序
    parts.sort((a, b) -> Integer.compare(a.partNumber(), b.partNumber()));
    List<CompletedPart> completed = new ArrayList<>();
    for (UploadedPart p : parts) {
      completed.add(CompletedPart.builder().partNumber(p.partNumber()).eTag(p.etag()).build());
    }
    s3.completeMultipartUpload(s.getObjectKey(), s.getS3UploadId(), completed);

    // upsert blob + 创建 entry
    byte[] sha = Hex.toBytes(sha256Hex);
    String key = s.getObjectKey();
    Blob blob = blobRepository.findBySha256AndSizeBytes(sha, s.getSizeBytes()).orElse(null);
    if (blob == null) {
      blob = new Blob();
      blob.setSha256(sha);
      blob.setSizeBytes(s.getSizeBytes());
      blob.setStorageKey(key);
      blob.setStatus(1);
      blob.setRefCount(0L);
      blobRepository.save(blob);
    }

    long entryId = createEntryByBlob(userId, s.getTargetParentId() == 0 ? null : s.getTargetParentId(), s.getTargetName(), blob, "fail");
    s.setStatus(3);
    s.setResultEntryId(entryId);
    s.setResultBlobId(blob.getId());
    uploadSessionRepository.save(s);
    return new CompleteResult(entryId, blob.getId());
  }

  @Transactional
  public void abort(long userId, String uploadId) {
    UploadSession s = uploadSessionRepository.findByUploadId(uploadId).orElseThrow(() -> BizException.notFound("upload_id 不存在"));
    if (s.getUserId() != userId) throw BizException.forbidden("无权限");
    if (s.getStatus() != null && s.getStatus() == 3) {
      throw BizException.conflict("上传已完成，不能取消");
    }
    if (s.getS3UploadId() != null && s.getObjectKey() != null) {
      s3.abortMultipartUpload(s.getObjectKey(), s.getS3UploadId());
    }
    s.setStatus(4);
    uploadSessionRepository.save(s);
  }

  private int normalizePartSize(Integer reqPartSize) {
    long min = props.getUpload().getPartSizeMinBytes();
    long max = props.getUpload().getPartSizeMaxBytes();
    long def = props.getUpload().getPartSizeDefaultBytes();
    long ps = reqPartSize == null ? def : reqPartSize.longValue();
    if (ps < min) ps = min;
    if (ps > max) ps = max;
    return (int) ps;
  }

  public record PrecheckResult(boolean needUpload, Long entryId, Integer partSize) {}

  public record PresignedPart(int partNumber, String url) {}

  public record InitUploadResult(
      boolean needUpload, String uploadId, Integer partSize, Integer totalParts, List<PresignedPart> parts, Long entryId) {
    public static InitUploadResult fast(long entryId) {
      return new InitUploadResult(false, null, null, null, null, entryId);
    }

    public static InitUploadResult multipart(String uploadId, int partSize, int totalParts, List<PresignedPart> parts) {
      return new InitUploadResult(true, uploadId, partSize, totalParts, parts, null);
    }
  }

  public record UploadedPart(int partNumber, String etag) {}

  public record CompleteResult(Long entryId, Long blobId) {}

  public record UploadSessionView(
      String uploadId,
      long targetParentId,
      String targetName,
      String sha256Hex,
      long sizeBytes,
      int partSize,
      int totalParts,
      String status,
      List<UploadedPart> uploadedParts) {
    static UploadSessionView from(UploadSession s, String shaHex, List<UploadedPart> parts) {
      String st =
          switch (s.getStatus() == null ? 0 : s.getStatus()) {
            case 1 -> "init";
            case 2 -> "uploading";
            case 3 -> "completed";
            case 4 -> "aborted";
            case 5 -> "expired";
            default -> "init";
          };
      return new UploadSessionView(
          s.getUploadId(),
          s.getTargetParentId(),
          s.getTargetName(),
          shaHex,
          s.getSizeBytes(),
          s.getPartSize(),
          s.getTotalParts(),
          st,
          parts);
    }
  }
}

