package com.lynn.overtime.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NumberUtil {
    /** 统一两位小数（四舍五入），全系统数值精度约定 */
    public static BigDecimal scale2(BigDecimal v) {
        if (v == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal of(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal of(String v) {
        if (v == null || v.trim().isEmpty()) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return new BigDecimal(v.trim()).setScale(2, RoundingMode.HALF_UP);
    }
}
