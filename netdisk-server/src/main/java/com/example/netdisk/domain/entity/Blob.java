package com.example.netdisk.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "blob",
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_blob_sha_size", columnNames = {"sha256", "size_bytes"}),
      @UniqueConstraint(name = "uk_blob_storage_key", columnNames = {"storage_key"})
    })
public class Blob {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, columnDefinition = "BINARY(32)")
  private byte[] sha256;

  @Column(name = "size_bytes", nullable = false)
  private Long sizeBytes;

  @Column(name = "mime_type", length = 128)
  private String mimeType;

  @Column(name = "storage_key", nullable = false, length = 512)
  private String storageKey;

  @Column(name = "ref_count", nullable = false)
  private Long refCount = 0L;

  @Column(nullable = false)
  private Integer status = 1; // 1=active 2=pending 3=quarantined 4=deleted

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();

  @PreUpdate
  public void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
