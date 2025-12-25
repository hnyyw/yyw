package com.example.netdisk.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "share",
    indexes = {@Index(name = "idx_share_owner", columnList = "owner_user_id,status,updated_at")},
    uniqueConstraints = {@UniqueConstraint(name = "uk_share_id", columnNames = {"share_id"})})
public class Share {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "share_id", nullable = false, length = 64)
  private String shareId;

  @Column(name = "owner_user_id", nullable = false)
  private Long ownerUserId;

  @Column(name = "root_entry_id", nullable = false)
  private Long rootEntryId;

  @Column(nullable = false)
  private Integer permission; // 1=preview 2=download 3=save

  @Column(name = "code_hash", length = 128)
  private String codeHash;

  @Column(name = "expired_at")
  private LocalDateTime expiredAt;

  @Column(nullable = false)
  private Integer status = 1; // 1=active 2=cancelled 3=expired

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();

  @PreUpdate
  public void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
