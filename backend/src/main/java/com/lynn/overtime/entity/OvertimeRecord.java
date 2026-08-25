package com.lynn.overtime.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "overtime_record")
public class OvertimeRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private LocalDate overtimeDate; // 加班日期

    private String weekday;          // 星期（自动推算）

    private String validPeriod;      // 有效时段，如 18:30-20:30

    @Column(nullable = false)
    private String type;             // 工作日 / 周末 / 法定节假日

    @Column(precision = 4, scale = 2, nullable = false)
    private BigDecimal ratio;       // 折算比例 0.5 / 1

    @Column(precision = 8, scale = 2, nullable = false)
    private BigDecimal validHours;   // 有效加班时长(h)

    @Column(precision = 8, scale = 2, nullable = false)
    private BigDecimal compHours;    // 转调休时长(h) = validHours * ratio

    private String punchTime;        // 实际打卡时间（选填）

    private String remark;           // 备注

    @Column(length = 7)
    private String month;            // 月份索引，如 2026-06
}
