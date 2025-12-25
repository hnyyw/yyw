package com.example.netdisk.dao;

import com.example.netdisk.domain.entity.Share;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShareRepository extends JpaRepository<Share, Long> {
  Optional<Share> findByShareId(String shareId);
}

