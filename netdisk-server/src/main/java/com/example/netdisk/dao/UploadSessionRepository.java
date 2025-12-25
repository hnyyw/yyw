package com.example.netdisk.dao;

import com.example.netdisk.domain.entity.UploadSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadSessionRepository extends JpaRepository<UploadSession, Long> {
  Optional<UploadSession> findByUploadId(String uploadId);
}

