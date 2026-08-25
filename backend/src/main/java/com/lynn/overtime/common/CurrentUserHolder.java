package com.lynn.overtime.common;

public class CurrentUserHolder {
    private static final ThreadLocal<AuthInfo> HOLDER = new ThreadLocal<>();

    public static void set(AuthInfo info) {
        HOLDER.set(info);
    }

    public static AuthInfo get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /** 仅 EMPLOYEE 角色返回关联成员 ID，否则抛异常（防越权） */
    public static Long requireMemberId() {
        AuthInfo info = get();
        if (info == null || info.getMemberId() == null) {
            throw new BizException("无权限访问该成员数据");
        }
        return info.getMemberId();
    }
}
