package com.lynn.overtime.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_message")
public class SysMessage {
    public static final int LEVEL_NORMAL = 1;
    public static final int LEVEL_HIGHLIGHT = 2; // 红色高亮

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type;             // 负余额 / 透支提醒 / 日历失败 / 操作

    @Column(length = 1000)
    private String content;

    private String receiverRole;     // 接收角色（空表示发给所有人）
    private Long receiverUser;       // 指定接收人（空表示按角色）

    @Column(nullable = false)
    private Integer level = LEVEL_NORMAL; // 1 普通 / 2 红色高亮

    @Column(nullable = false)
    private Integer isRead = 0;     // 0 未读 / 1 已读

    private LocalDateTime createdAt;

    @PrePersist
    void init() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (isRead == null) isRead = 0;
        if (level == null) level = LEVEL_NORMAL;
    }
}
