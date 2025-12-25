package com.example.netdisk.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "entry",
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_entry_name", columnNames = {"user_id", "parent_id", "name", "is_deleted"})
    },
    indexes = {
      @Index(name = "idx_entry_list", columnList = "user_id,parent_id,is_deleted,updated_at"),
      @Index(name = "idx_entry_trash", columnList = "user_id,is_deleted,deleted_at")
    })
public class Entry {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "parent_id")
  private Long parentId;

  @Column(nullable = false)
  private Integer type; // 1=folder 2=file

  @Column(nullable = false, length = 255)
  private String name;

  @Column(name = "blob_id")
  private Long blobId;

  @Column(name = "size_bytes", nullable = false)
  private Long sizeBytes = 0L;

  @Column(name = "is_deleted", nullable = false)
  private Integer isDeleted = 0;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();

  @PreUpdate
  public void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
