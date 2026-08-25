package com.lynn.overtime.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_user", uniqueConstraints = {@UniqueConstraint(columnNames = "username")})
public class SysUser {
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_CLERK = "CLERK";
    public static final String ROLE_READONLY = "READONLY";
    public static final String ROLE_EMPLOYEE = "EMPLOYEE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 26)
    private String username;         // 登录名（字母+数字）

    @Column(nullable = false)
    private String passwordHash;     // BCrypt 哈希

    @Column(nullable = false)
    private String role;             // ADMIN / CLERK / READONLY / EMPLOYEE

    private Long memberId;           // 员工自助账号关联的成员（其他角色为 null）

    @Column(nullable = false)
    private Integer status = 1;      // 1 启用 / 0 禁用

    private String sessionToken;     // 当前会话 token（单设备登录）
    private LocalDateTime sessionExpireAt;

    @Column(nullable = false)
    private Boolean mustChangePwd = false; // 首次登录强制改密

    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    @PrePersist
    void init() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = 1;
        if (mustChangePwd == null) mustChangePwd = false;
    }
}
