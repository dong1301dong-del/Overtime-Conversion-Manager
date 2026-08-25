package com.lynn.overtime.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "holiday_calendar", uniqueConstraints = {@UniqueConstraint(columnNames = "holiday_date")})
public class HolidayCalendar {
    /** 法定节假日（ratio=1） */
    public static final String KIND_HOLIDAY = "HOLIDAY";
    /** 手动标记为工作日（ratio=0.5，覆盖周末） */
    public static final String KIND_WORKDAY = "WORKDAY";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate holidayDate;

    private String name;             // 名称，如 元旦

    @Column(nullable = false)
    private String kind;             // HOLIDAY / WORKDAY

    private Integer auto = 0;        // 1=系统自动更新 / 0=手动
}
