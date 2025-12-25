package com.example.netdisk.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_quota")
public class UserQuota {
  @Id
  @Column(name = "user_id")
  private Long userId;

  @Column(name = "quota_bytes", nullable = false)
  private Long quotaBytes;

  @Column(name = "used_bytes", nullable = false)
  private Long usedBytes = 0L;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();
}
