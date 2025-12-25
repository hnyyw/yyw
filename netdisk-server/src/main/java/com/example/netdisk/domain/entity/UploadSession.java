package com.example.netdisk.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "upload_session",
    indexes = {@Index(name = "idx_upload_user", columnList = "user_id,status,updated_at")},
    uniqueConstraints = {@UniqueConstraint(name = "uk_upload_id", columnNames = {"upload_id"})})
public class UploadSession {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "upload_id", nullable = false, length = 64)
  private String uploadId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "target_parent_id", nullable = false)
  private Long targetParentId;

  @Column(name = "target_name", nullable = false, length = 255)
  private String targetName;

  @Column(nullable = false, columnDefinition = "BINARY(32)")
  private byte[] sha256;

  @Column(name = "size_bytes", nullable = false)
  private Long sizeBytes;

  @Column(name = "part_size", nullable = false)
  private Integer partSize;

  @Column(name = "total_parts", nullable = false)
  private Integer totalParts;

  @Column(name = "object_key", length = 512)
  private String objectKey;

  @Column(name = "s3_upload_id", length = 256)
  private String s3UploadId;

  @Column(nullable = false)
  private Integer status = 1; // 1=init 2=uploading 3=completed 4=aborted 5=expired

  @Column(name = "result_entry_id")
  private Long resultEntryId;

  @Column(name = "result_blob_id")
  private Long resultBlobId;

  @Column(name = "expired_at", nullable = false)
  private LocalDateTime expiredAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();

  @PreUpdate
  public void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
