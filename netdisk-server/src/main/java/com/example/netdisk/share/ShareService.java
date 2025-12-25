package com.example.netdisk.share;

import com.example.netdisk.common.BizException;
import com.example.netdisk.config.NetdiskProperties;
import com.example.netdisk.dao.BlobRepository;
import com.example.netdisk.dao.EntryRepository;
import com.example.netdisk.dao.ShareRepository;
import com.example.netdisk.domain.entity.Entry;
import com.example.netdisk.domain.entity.Share;
import com.example.netdisk.upload.S3Facade;
import com.example.netdisk.upload.UploadService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShareService {
  private final ShareRepository shareRepository;
  private final EntryRepository entryRepository;
  private final BlobRepository blobRepository;
  private final ShareTokenStore tokenStore;
  private final PasswordEncoder passwordEncoder;
  private final NetdiskProperties props;
  private final S3Facade s3;
  private final UploadService uploadService;

  @Transactional
  public Share create(long ownerUserId, long rootEntryId, String permission, String code, LocalDateTime expiredAt) {
    Entry root =
        entryRepository
            .findByIdAndUserId(rootEntryId, ownerUserId)
            .orElseThrow(() -> BizException.notFound("文件不存在"));
    if (root.getIsDeleted() != null && root.getIsDeleted() == 1) {
      throw BizException.conflict("回收站文件不能分享");
    }
    int perm =
        switch (permission) {
          case "preview" -> 1;
          case "download" -> 2;
          case "save" -> 3;
          default -> throw BizException.badRequest("permission 不合法");
        };
    Share s = new Share();
    s.setShareId("s_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
    s.setOwnerUserId(ownerUserId);
    s.setRootEntryId(rootEntryId);
    s.setPermission(perm);
    if (code != null && !code.isBlank()) {
      s.setCodeHash(passwordEncoder.encode(code));
    }
    s.setExpiredAt(expiredAt);
    s.setStatus(1);
    return shareRepository.save(s);
  }

  public String verify(String shareId, String code) {
    Share s = shareRepository.findByShareId(shareId).orElseThrow(() -> BizException.notFound("分享不存在"));
    if (s.getStatus() == null || s.getStatus() != 1) throw BizException.conflict("分享已失效");
    if (s.getExpiredAt() != null && s.getExpiredAt().isBefore(LocalDateTime.now())) {
      s.setStatus(3);
      shareRepository.save(s);
      throw BizException.conflict("分享已过期");
    }
    if (s.getCodeHash() != null && !s.getCodeHash().isBlank()) {
      if (code == null || !passwordEncoder.matches(code, s.getCodeHash())) {
        throw BizException.unauthorized("提取码错误");
      }
    }
    String token = "st_" + UUID.randomUUID().toString().replace("-", "");
    tokenStore.put(token, s.getShareId(), s.getPermission(), props.getPresign().getShareTokenTtlSeconds());
    return token;
  }

  public Share requireActiveShare(String shareId) {
    Share s = shareRepository.findByShareId(shareId).orElseThrow(() -> BizException.notFound("分享不存在"));
    if (s.getStatus() == null || s.getStatus() != 1) throw BizException.conflict("分享已失效");
    if (s.getExpiredAt() != null && s.getExpiredAt().isBefore(LocalDateTime.now())) {
      s.setStatus(3);
      shareRepository.save(s);
      throw BizException.conflict("分享已过期");
    }
    return s;
  }

  public ShareTokenStore.ShareToken requireShareToken(String shareId, String token) {
    if (token == null || token.isBlank()) throw BizException.unauthorized("缺少 share_token");
    ShareTokenStore.ShareToken t = tokenStore.get(token);
    if (t == null) throw BizException.unauthorized("share_token 无效或过期");
    if (!shareId.equals(t.shareId())) throw BizException.unauthorized("share_token 不匹配");
    return t;
  }

  public List<Map<String, Object>> list(String shareId, String token, Long parentEntryId) {
    Share s = requireActiveShare(shareId);
    ShareTokenStore.ShareToken t = requireShareToken(shareId, token);
    // token 中 permission 只做下限校验：存在即可；具体下载/转存再细分
    if (t.permission() < 1) throw BizException.forbidden("无权限");

    Entry root = entryRepository.findById(s.getRootEntryId()).orElseThrow(() -> BizException.notFound("分享内容不存在"));
    Long pid = (parentEntryId == null || parentEntryId == 0) ? root.getId() : parentEntryId;

    // root 是文件：只返回自身
    if (root.getType() != null && root.getType() == 2) {
      return List.of(toShareVo(root));
    }

    // 子树校验：pid 必须在 root 子树下（含 root）
    Entry parent = entryRepository.findById(pid).orElseThrow(() -> BizException.notFound("目录不存在"));
    if (!isInSubtree(parent, root.getId())) {
      throw BizException.forbidden("越权访问");
    }
    List<Entry> children = entryRepository.findByUserIdAndParentIdAndIsDeleted(root.getUserId(), pid, 0);
    return children.stream().map(this::toShareVo).toList();
  }

  public String shareDownloadLink(String shareId, String token, long entryId) {
    Share s = requireActiveShare(shareId);
    ShareTokenStore.ShareToken t = requireShareToken(shareId, token);
    if (t.permission() < 2) throw BizException.forbidden("无下载权限");
    Entry root = entryRepository.findById(s.getRootEntryId()).orElseThrow(() -> BizException.notFound("分享内容不存在"));
    Entry e = entryRepository.findById(entryId).orElseThrow(() -> BizException.notFound("文件不存在"));
    if (!isInSubtree(e, root.getId())) throw BizException.forbidden("越权访问");
    if (e.getType() == null || e.getType() != 2 || e.getBlobId() == null) throw BizException.badRequest("不是文件");
    var blob = blobRepository.findById(e.getBlobId()).orElseThrow(() -> BizException.notFound("内容不存在"));
    return s3.presignDownloadUrl(blob.getStorageKey(), props.getPresign().getDownloadExpireSeconds());
  }

  @Transactional
  public void saveToMyDrive(String shareId, String token, long entryId, long targetUserId, Long targetParentId) {
    Share s = requireActiveShare(shareId);
    ShareTokenStore.ShareToken t = requireShareToken(shareId, token);
    if (t.permission() < 3) throw BizException.forbidden("无转存权限");
    Entry root = entryRepository.findById(s.getRootEntryId()).orElseThrow(() -> BizException.notFound("分享内容不存在"));
    Entry e = entryRepository.findById(entryId).orElseThrow(() -> BizException.notFound("文件不存在"));
    if (!isInSubtree(e, root.getId())) throw BizException.forbidden("越权访问");
    if (e.getType() == null || e.getType() != 2 || e.getBlobId() == null) throw BizException.badRequest("仅支持转存文件");
    var blob = blobRepository.findById(e.getBlobId()).orElseThrow(() -> BizException.notFound("内容不存在"));
    uploadService.createEntryByBlob(targetUserId, targetParentId, e.getName(), blob, "auto_rename");
  }

  @Transactional
  public void cancel(long ownerUserId, String shareId) {
    Share s = shareRepository.findByShareId(shareId).orElseThrow(() -> BizException.notFound("分享不存在"));
    if (s.getOwnerUserId() != ownerUserId) throw BizException.forbidden("无权限");
    s.setStatus(2);
    shareRepository.save(s);
  }

  private boolean isInSubtree(Entry node, long rootId) {
    long cur = node.getId();
    int guard = 0;
    while (true) {
      if (cur == rootId) return true;
      if (guard++ > 64) return false;
      Entry e = entryRepository.findById(cur).orElse(null);
      if (e == null) return false;
      Long p = e.getParentId();
      if (p == null) return false;
      cur = p;
    }
  }

  private Map<String, Object> toShareVo(Entry e) {
    String type = (e.getType() != null && e.getType() == 1) ? "folder" : "file";
    return Map.of("id", e.getId(), "parent_id", e.getParentId(), "type", type, "name", e.getName(), "size_bytes", e.getSizeBytes());
  }
}

