package com.lynn.overtime.service;

import com.lynn.overtime.entity.SysConfig;
import com.lynn.overtime.repository.SysConfigRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ConfigService {

    public static final String KEY_STD_WORK_HOURS = "std_work_hours";   // 标准工时（h/天）
    public static final String KEY_PRECISION = "precision";             // 数值精度
    public static final String KEY_BACKUP_RETENTION = "backup_retention"; // 备份保留份数
    public static final String KEY_SESSION_TIMEOUT = "session_timeout_hours";

    private final SysConfigRepository configRepo;

    public ConfigService(SysConfigRepository configRepo) {
        this.configRepo = configRepo;
    }

    public String get(String key, String def) {
        return configRepo.findByConfKey(key).map(SysConfig::getConfValue).orElse(def);
    }

    public void set(String key, String value, String description) {
        SysConfig c = configRepo.findByConfKey(key).orElse(new SysConfig());
        c.setConfKey(key);
        c.setConfValue(value);
        if (description != null) c.setDescription(description);
        configRepo.save(c);
    }

    public BigDecimal stdWorkHours() {
        return new BigDecimal(get(KEY_STD_WORK_HOURS, "7.5"));
    }

    public int precision() {
        try {
            return Integer.parseInt(get(KEY_PRECISION, "2"));
        } catch (Exception e) {
            return 2;
        }
    }

    public int backupRetention() {
        try {
            return Integer.parseInt(get(KEY_BACKUP_RETENTION, "10"));
        } catch (Exception e) {
            return 10;
        }
    }

    public int sessionTimeoutHours() {
        try {
            return Integer.parseInt(get(KEY_SESSION_TIMEOUT, "24"));
        } catch (Exception e) {
            return 24;
        }
    }

    public java.util.List<SysConfig> listAll() {
        return configRepo.findAll();
    }
}
