package com.example.netdisk.dao;

import com.example.netdisk.domain.entity.Blob;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlobRepository extends JpaRepository<Blob, Long> {
  Optional<Blob> findBySha256AndSizeBytes(byte[] sha256, Long sizeBytes);
}

