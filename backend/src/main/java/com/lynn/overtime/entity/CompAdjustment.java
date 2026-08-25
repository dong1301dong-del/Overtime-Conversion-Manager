package com.lynn.overtime.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "comp_adjustment")
public class CompAdjustment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private LocalDate date;          // 调休产生日期

    @Column(precision = 8, scale = 2, nullable = false)
    private BigDecimal hours;        // 其他调休时长(h)，两位小数

    @Column(nullable = false)
    private String category;         // 项目奖励 / 领导奖励 / 公司福利 / 其他

    private String remark;

    @Column(length = 7)
    private String month;            // 月份索引，如 2026-06
}
