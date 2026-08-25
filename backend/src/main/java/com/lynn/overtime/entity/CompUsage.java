package com.lynn.overtime.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "comp_usage")
public class CompUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private LocalDate useStart;      // 使用开始日期

    private LocalDate useEnd;        // 使用结束日期

    @Column(nullable = false)
    private Integer mode;            // 1=小时(A) / 2=天数(B/C)

    @Column(precision = 8, scale = 2)
    private BigDecimal hours;        // 折算后使用小时数（自动计算，两位小数）

    @Column(precision = 4, scale = 1)
    private BigDecimal days;         // 录入天数（仅模式 B/C 有值，0.5 倍数）

    @Column(nullable = false)
    private Integer isOverdraft = 0; // 1 透支 / 0 否

    private String remark;           // 自动 "透支调休" 或手工

    @Column(length = 7)
    private String month;            // 月份索引，如 2026-06
}
