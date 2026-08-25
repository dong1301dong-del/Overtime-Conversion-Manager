package com.lynn.overtime.common;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 从 {@code @RequestBody Map<String,Object>} 中安全提取并转换字段。
 * 前端统一以 JSON 提交，数值可能为 Integer/Long/Double/BigDecimal/字符串，日期为字符串。
 */
public class Req {

    public static String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : String.valueOf(v).trim();
    }

    public static String str(Map<String, Object> m, String k, String def) {
        String v = str(m, k);
        return (v == null || v.isEmpty()) ? def : v;
    }

    public static BigDecimal dec(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v == null) return null;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return BigDecimal.valueOf(((Number) v).doubleValue());
        return NumberUtil.of(String.valueOf(v));
    }

    public static Integer intg(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        return Integer.parseInt(String.valueOf(v).trim());
    }

    public static Long longg(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        return Long.parseLong(String.valueOf(v).trim());
    }

    public static Boolean bool(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v == null) return null;
        if (v instanceof Boolean) return (Boolean) v;
        return "true".equalsIgnoreCase(String.valueOf(v));
    }

    public static LocalDate date(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v == null) return null;
        if (v instanceof LocalDate) return (LocalDate) v;
        return DateUtil.parse(String.valueOf(v));
    }
}
