package com.lynn.overtime.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String username;
    private String action;           // 操作类型
    @Column(length = 2000)
    private String detail;           // 操作详情
    private LocalDateTime createdAt;

    @PrePersist
    void init() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
