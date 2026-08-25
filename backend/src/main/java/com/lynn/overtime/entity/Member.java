package com.lynn.overtime.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "member", uniqueConstraints = {@UniqueConstraint(columnNames = "username")})
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;            // 姓名（显示名，可中文）

    @Column(nullable = false, unique = true, length = 26)
    private String username;        // 登录名（字母+数字，≤26，不允许中文）

    private String department;      // 部门

    @Column(nullable = false)
    private Integer status = 1;     // 1 启用 / 0 禁用

    private LocalDateTime createdAt;

    @PrePersist
    void init() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = 1;
    }
}
