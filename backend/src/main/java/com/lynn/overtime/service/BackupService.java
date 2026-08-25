package com.lynn.overtime.service;

import com.lynn.overtime.common.NumberUtil;
import com.lynn.overtime.entity.*;
import com.lynn.overtime.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 数据库备份：优先使用 mysqldump 导出 .sql 并压缩；
 * 若环境无 mysqldump，则降级为全表 JSON 导出压缩。
 * 按配置保留最新 N 份（默认 10）。
 */
@Service
public class BackupService {

    private final BackupLogRepository backupLogRepo;
    private final ConfigService configService;
    private final MemberRepository memberRepo;
    private final OvertimeRecordRepository overtimeRepo;
    private final CompUsageRepository usageRepo;
    private final CompAdjustmentRepository adjustRepo;
    private final HolidayCalendarRepository holidayRepo;
    private final SysUserRepository userRepo;
    private final SysMessageRepository msgRepo;
    private final AuditLogRepository auditRepo;

    @Value("${spring.datasource.url}")
    private String dbUrl;
    @Value("${spring.datasource.username}")
    private String dbUser;
    @Value("${spring.datasource.password}")
    private String dbPassword;
    @Value("${app.backup-dir}")
    private String backupDir;
    @Value("${app.data-dir}")
    private String dataDir;

    public BackupService(BackupLogRepository backupLogRepo, ConfigService configService,
                         MemberRepository memberRepo, OvertimeRecordRepository overtimeRepo,
                         CompUsageRepository usageRepo, CompAdjustmentRepository adjustRepo,
                         HolidayCalendarRepository holidayRepo, SysUserRepository userRepo,
                         SysMessageRepository msgRepo, AuditLogRepository auditRepo) {
        this.backupLogRepo = backupLogRepo;
        this.configService = configService;
        this.memberRepo = memberRepo;
        this.overtimeRepo = overtimeRepo;
        this.usageRepo = usageRepo;
        this.adjustRepo = adjustRepo;
        this.holidayRepo = holidayRepo;
        this.userRepo = userRepo;
        this.msgRepo = msgRepo;
        this.auditRepo = auditRepo;
    }

    public synchronized BackupLog backupNow() {
        try {
            Files.createDirectories(Path.of(backupDir));
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String dbName = parseDbName(dbUrl);
            Path sqlFile = Path.of(dataDir, "backup_tmp_" + ts + ".sql");
            Path zipFile = Path.of(backupDir, "backup_" + ts + ".zip");

            boolean dumped = dumpWithMysqldump(dbName, sqlFile);
            Path payload;
            if (dumped && Files.exists(sqlFile) && Files.size(sqlFile) > 0) {
                payload = sqlFile;
            } else {
                payload = exportJson(ts); // 降级：JSON
            }
            zip(payload, zipFile);
            // 清理临时 sql
            try { Files.deleteIfExists(sqlFile); } catch (Exception ignored) {}

            BackupLog log = new BackupLog();
            log.setFilename(zipFile.getFileName().toString());
            log.setSize(Files.size(zipFile));
            log.setNote(dumped ? "mysqldump" : "json-fallback");
            log.setCreatedAt(LocalDateTime.now());
            BackupLog saved = backupLogRepo.save(log);

            prune();
            return saved;
        } catch (Exception e) {
            throw new com.lynn.overtime.common.BizException("备份失败：" + e.getMessage());
        }
    }

    private boolean dumpWithMysqldump(String dbName, Path out) {
        String exe = findMysqldump();
        if (exe == null) return false;
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(exe);
            cmd.add("-u" + dbUser);
            if (dbPassword != null && !dbPassword.isEmpty()) cmd.add("-p" + dbPassword);
            cmd.add("--single-transaction");
            cmd.add("--routines");
            cmd.add("--default-character-set=utf8mb4");
            cmd.add(dbName);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.redirectOutput(out.toFile());
            Process p = pb.start();
            int code = p.waitFor();
            return code == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String findMysqldump() {
        String cfg = configService.get("mysqldump_path", "");
        if (!cfg.isEmpty() && new File(cfg).exists()) return cfg;
        String env = System.getenv("MYSQLDUMP_PATH");
        if (env != null && !env.isEmpty() && new File(env).exists()) return env;
        String[] candidates = {
                "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysqldump.exe",
                "C:\\Program Files\\MySQL\\MySQL Server 8.4\\bin\\mysqldump.exe",
                "C:\\Program Files\\MySQL\\MySQL Server 9.0\\bin\\mysqldump.exe",
                "C:\\Program Files\\MySQL\\MySQL Server 5.7\\bin\\mysqldump.exe"
        };
        for (String c : candidates) {
            if (new File(c).exists()) return c;
        }
        // 末选：依赖 PATH
        try {
            Process p = new ProcessBuilder("where", "mysqldump").start();
            if (p.waitFor() == 0) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line = br.readLine();
                    if (line != null && !line.isEmpty()) return line.trim();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Path exportJson(String ts) throws IOException {
        Path json = Path.of(dataDir, "backup_tmp_" + ts + ".json");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("member", memberRepo.findAll());
        data.put("overtime_record", overtimeRepo.findAll());
        data.put("comp_usage", usageRepo.findAll());
        data.put("comp_adjustment", adjustRepo.findAll());
        data.put("holiday_calendar", holidayRepo.findAll());
        data.put("sys_user", userRepo.findAll());
        data.put("sys_message", msgRepo.findAll());
        data.put("audit_log", auditRepo.findAll());
        data.put("backup_log", backupLogRepo.findAll());
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        om.writerWithDefaultPrettyPrinter().writeValue(json.toFile(), data);
        return json;
    }

    private void zip(Path src, Path zip) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            zos.putNextEntry(new ZipEntry(src.getFileName().toString()));
            Files.copy(src, zos);
            zos.closeEntry();
        }
    }

    private void prune() {
        int retention = configService.backupRetention();
        List<BackupLog> logs = backupLogRepo.findTop20ByOrderByCreatedAtDesc();
        if (logs.size() <= retention) return;
        for (int i = retention; i < logs.size(); i++) {
            BackupLog l = logs.get(i);
            try { Files.deleteIfExists(Path.of(backupDir, l.getFilename())); } catch (Exception ignored) {}
            backupLogRepo.delete(l);
        }
    }

    private String parseDbName(String url) {
        // jdbc:mysql://host:port/db?params
        int q = url.indexOf('?');
        String base = q >= 0 ? url.substring(0, q) : url;
        int slash = base.lastIndexOf('/');
        return slash >= 0 ? base.substring(slash + 1) : "overtime_db";
    }
}
