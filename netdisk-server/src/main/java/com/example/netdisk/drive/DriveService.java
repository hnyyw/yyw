package com.example.netdisk.drive;

import com.example.netdisk.common.BizException;
import com.example.netdisk.dao.EntryRepository;
import com.example.netdisk.domain.entity.Entry;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DriveService {
  private final EntryRepository entryRepository;

  public String ensureUniqueName(long userId, Long parentId, String name) {
    if (!entryRepository.existsByUserIdAndParentIdAndNameAndIsDeleted(userId, parentId, name, 0)) {
      return name;
    }
    String base = name;
    String ext = "";
    int dot = name.lastIndexOf('.');
    if (dot > 0) {
      base = name.substring(0, dot);
      ext = name.substring(dot);
    }
    for (int i = 1; i <= 9999; i++) {
      String candidate = base + "(" + i + ")" + ext;
      if (!entryRepository.existsByUserIdAndParentIdAndNameAndIsDeleted(userId, parentId, candidate, 0)) {
        return candidate;
      }
    }
    throw BizException.conflict("同名文件过多，请换个名字");
  }

  @Transactional
  public long mkdir(long userId, Long parentId, String name) {
    Entry e = new Entry();
    e.setUserId(userId);
    e.setParentId(normalizeParent(parentId));
    e.setType(1);
    e.setName(name);
    e.setBlobId(null);
    e.setSizeBytes(0L);
    e.setIsDeleted(0);
    try {
      return entryRepository.save(e).getId();
    } catch (DataIntegrityViolationException ex) {
      throw BizException.conflict("同目录已存在同名文件/文件夹");
    }
  }

  public Entry requireOwnedEntry(long userId, long entryId) {
    return entryRepository.findByIdAndUserId(entryId, userId).orElseThrow(() -> BizException.notFound("文件不存在"));
  }

  @Transactional
  public void rename(long userId, long entryId, String newName) {
    Entry e = requireOwnedEntry(userId, entryId);
    if (e.getIsDeleted() != null && e.getIsDeleted() == 1) {
      throw BizException.conflict("回收站文件不支持重命名");
    }
    e.setName(newName);
    try {
      entryRepository.save(e);
    } catch (DataIntegrityViolationException ex) {
      throw BizException.conflict("同目录已存在同名文件/文件夹");
    }
  }

  @Transactional
  public void move(long userId, List<Long> entryIds, Long targetParentId) {
    Long pid = normalizeParent(targetParentId);
    for (Long id : entryIds) {
      Entry e = requireOwnedEntry(userId, id);
      if (e.getIsDeleted() != null && e.getIsDeleted() == 1) {
        throw BizException.conflict("回收站文件不支持移动");
      }
      e.setParentId(pid);
      try {
        entryRepository.save(e);
      } catch (DataIntegrityViolationException ex) {
        throw BizException.conflict("移动失败：目标目录存在同名项");
      }
    }
  }

  @Transactional
  public void deleteToTrash(long userId, List<Long> entryIds) {
    for (Long id : entryIds) {
      Entry e = requireOwnedEntry(userId, id);
      softDeleteRecursive(userId, e);
    }
  }

  private void softDeleteRecursive(long userId, Entry e) {
    if (e.getIsDeleted() != null && e.getIsDeleted() == 1) {
      return;
    }
    e.setIsDeleted(1);
    e.setDeletedAt(LocalDateTime.now());
    entryRepository.save(e);
    if (e.getType() != null && e.getType() == 1) {
      List<Entry> children = entryRepository.findByUserIdAndParentId(userId, e.getId());
      for (Entry c : children) {
        softDeleteRecursive(userId, c);
      }
    }
  }

  @Transactional
  public void restoreFromTrash(long userId, List<Long> entryIds, String strategy) {
    for (Long id : entryIds) {
      Entry e = requireOwnedEntry(userId, id);
      if (e.getIsDeleted() == null || e.getIsDeleted() == 0) {
        continue;
      }
      String name = e.getName();
      if ("auto_rename".equals(strategy)) {
        name = ensureUniqueName(userId, e.getParentId(), name);
      } else if ("fail".equals(strategy)) {
        if (entryRepository.existsByUserIdAndParentIdAndNameAndIsDeleted(userId, e.getParentId(), name, 0)) {
          throw BizException.conflict("还原失败：同目录存在同名项");
        }
      } else if ("overwrite".equals(strategy)) {
        // MVP：overwrite 语义复杂（需要删除目标同名项及其子树），先作为 fail 处理或后续增强
        if (entryRepository.existsByUserIdAndParentIdAndNameAndIsDeleted(userId, e.getParentId(), name, 0)) {
          throw BizException.conflict("还原失败：目标存在同名项（overwrite MVP 未实现）");
        }
      }
      restoreRecursive(userId, e, name);
    }
  }

  private void restoreRecursive(long userId, Entry e, String restoredName) {
    e.setIsDeleted(0);
    e.setDeletedAt(null);
    e.setName(restoredName);
    entryRepository.save(e);
    if (e.getType() != null && e.getType() == 1) {
      List<Entry> children = entryRepository.findByUserIdAndParentId(userId, e.getId());
      for (Entry c : children) {
        if (c.getIsDeleted() != null && c.getIsDeleted() == 1) {
          // 子项恢复同名冲突：auto_rename
          String childName = ensureUniqueName(userId, c.getParentId(), c.getName());
          restoreRecursive(userId, c, childName);
        }
      }
    }
  }

  private Long normalizeParent(Long parentId) {
    if (parentId == null || parentId == 0) return null;
    return parentId;
  }

  public List<Entry> listFolder(long userId, Long parentId, boolean includeDeleted) {
    Long pid = normalizeParent(parentId);
    int isDeleted = includeDeleted ? 1 : 0;
    return entryRepository.findByUserIdAndParentIdAndIsDeleted(userId, pid, isDeleted);
  }

  public List<Entry> listTrash(long userId) {
    // 预留：Controller 走分页查询，这里不提供实现
    return new ArrayList<>();
  }
}
