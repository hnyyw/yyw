package com.example.netdisk.dao;

import com.example.netdisk.domain.entity.Entry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntryRepository extends JpaRepository<Entry, Long> {
  Page<Entry> findByUserIdAndParentIdAndIsDeleted(Long userId, Long parentId, Integer isDeleted, Pageable pageable);

  Page<Entry> findByUserIdAndIsDeleted(Long userId, Integer isDeleted, Pageable pageable);

  List<Entry> findByUserIdAndParentIdAndIsDeleted(Long userId, Long parentId, Integer isDeleted);

  boolean existsByUserIdAndParentIdAndNameAndIsDeleted(Long userId, Long parentId, String name, Integer isDeleted);

  Optional<Entry> findByIdAndUserId(Long id, Long userId);

  List<Entry> findByUserIdAndParentId(Long userId, Long parentId);
}

