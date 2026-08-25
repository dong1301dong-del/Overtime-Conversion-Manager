package com.lynn.overtime.common;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordUtil {
    private static final BCryptPasswordEncoder ENC = new BCryptPasswordEncoder();

    public static String encode(String raw) {
        return ENC.encode(raw);
    }

    public static boolean matches(String raw, String hash) {
        if (raw == null || hash == null) return false;
        return ENC.matches(raw, hash);
    }

    /**
     * 密码强度校验：长度 8-20，含大写+小写+数字+特殊字符，不与用户名相同。
     * 正则：^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\w\s]).{8,20}$
     */
    public static final String PWD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w\\s]).{8,20}$";

    public static boolean isStrong(String password) {
        return password != null && password.matches(PWD_REGEX);
    }

    public static boolean sameAsUsername(String password, String username) {
        return password != null && password.equals(username);
    }
}
