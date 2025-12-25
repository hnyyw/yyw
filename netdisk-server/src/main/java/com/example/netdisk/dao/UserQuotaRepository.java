package com.example.netdisk.dao;

import com.example.netdisk.domain.entity.UserQuota;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserQuotaRepository extends JpaRepository<UserQuota, Long> {}

