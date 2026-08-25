package com.lynn.overtime.common;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateUtil {
    private static final DateTimeFormatter[] FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    };

    public static LocalDate parse(String s) {
        if (s == null) return null;
        String t = s.trim();
        for (DateTimeFormatter f : FORMATTERS) {
            try {
                return LocalDate.parse(t, f);
            } catch (DateTimeParseException ignored) {
            }
        }
        // 尝试纯数字 yyyyMMdd
        try {
            if (t.matches("\\d{8}")) {
                return LocalDate.parse(t, DateTimeFormatter.ofPattern("yyyyMMdd"));
            }
        } catch (Exception ignored) {
        }
        throw new BizException("日期格式错误：" + s + "（应为 YYYY/MM/DD 或 YYYY-MM-DD）");
    }

    public static String weekdayCN(LocalDate d) {
        DayOfWeek dow = d.getDayOfWeek();
        switch (dow) {
            case MONDAY: return "星期一";
            case TUESDAY: return "星期二";
            case WEDNESDAY: return "星期三";
            case THURSDAY: return "星期四";
            case FRIDAY: return "星期五";
            case SATURDAY: return "星期六";
            case SUNDAY: return "星期日";
            default: return "";
        }
    }

    public static String monthOf(LocalDate d) {
        return String.format("%04d-%02d", d.getYear(), d.getMonthValue());
    }
}
