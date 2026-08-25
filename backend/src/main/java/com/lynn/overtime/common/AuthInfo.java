package com.lynn.overtime.common;

import lombok.Data;

@Data
public class AuthInfo {
    private Long userId;
    private String username;
    private String role;
    private Long memberId;

    public AuthInfo(Long userId, String username, String role, Long memberId) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.memberId = memberId;
    }
}
