package com.lynn.overtime.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "sys_config", uniqueConstraints = {@UniqueConstraint(columnNames = "conf_key")})
public class SysConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String confKey;

    private String confValue;

    private String description;
}
