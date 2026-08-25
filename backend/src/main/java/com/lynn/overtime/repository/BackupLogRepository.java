package com.lynn.overtime.repository;

import com.lynn.overtime.entity.BackupLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BackupLogRepository extends JpaRepository<BackupLog, Long> {
    List<BackupLog> findTop20ByOrderByCreatedAtDesc();
}
