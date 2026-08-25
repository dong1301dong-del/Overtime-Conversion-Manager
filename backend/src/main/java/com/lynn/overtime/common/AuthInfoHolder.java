package com.lynn.overtime.common;

import com.lynn.overtime.entity.AuditLog;
import com.lynn.overtime.repository.AuditLogRepository;

public class AuthInfoHolder {
    /** 写入审计日志，自动带入当前操作人 */
    public static void log(AuditLogRepository auditRepo, String action, String detail) {
        try {
            AuditLog l = new AuditLog();
            AuthInfo info = CurrentUserHolder.get();
            if (info != null) {
                l.setUserId(info.getUserId());
                l.setUsername(info.getUsername());
            }
            l.setAction(action);
            l.setDetail(detail);
            auditRepo.save(l);
        } catch (Exception ignored) {
            // 审计失败不应阻断主流程
        }
    }
}
