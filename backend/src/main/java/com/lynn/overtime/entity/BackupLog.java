package com.lynn.overtime.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "backup_log")
public class BackupLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;         // backup_YYYYMMDD_HHmmss.zip

    private Long size;               // 字节

    @Column(length = 2000)
    private String note;

    private LocalDateTime createdAt;

    @PrePersist
    void init() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
