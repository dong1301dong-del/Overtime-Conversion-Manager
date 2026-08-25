package com.lynn.overtime.common;

import com.lynn.overtime.entity.SysUser;

/**
 * 基于 ThreadLocal 中当前登录信息的角色校验工具。
 */
public class SecurityUtil {

    public static String currentRole() {
        AuthInfo info = CurrentUserHolder.get();
        if (info == null) throw new BizException("未登录");
        return info.getRole();
    }

    public static Long currentUserId() {
        AuthInfo info = CurrentUserHolder.get();
        if (info == null) throw new BizException("未登录");
        return info.getUserId();
    }

    /** 员工自助账号返回其关联成员 ID，否则抛异常（防越权） */
    public static Long currentMemberId() {
        return CurrentUserHolder.requireMemberId();
    }

    /** 当前角色不在允许列表中则抛无权限异常 */
    public static void require(String... roles) {
        String r = currentRole();
        for (String role : roles) {
            if (role.equals(r)) return;
        }
        throw new BizException("无权限执行此操作");
    }

    public static boolean isAdmin() {
        return SysUser.ROLE_ADMIN.equals(currentRole());
    }
}
