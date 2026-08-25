package com.lynn.overtime.config;

import com.lynn.overtime.common.PasswordUtil;
import com.lynn.overtime.entity.SysUser;
import com.lynn.overtime.repository.HolidayCalendarRepository;
import com.lynn.overtime.repository.SysUserRepository;
import com.lynn.overtime.service.ConfigService;
import com.lynn.overtime.service.HolidayService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 首次启动种子数据：管理员账号、系统默认配置、当年及下一年节假日。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final SysUserRepository userRepo;
    private final ConfigService configService;
    private final HolidayService holidayService;
    private final HolidayCalendarRepository holidayRepo;

    public DataInitializer(SysUserRepository userRepo, ConfigService configService,
                           HolidayService holidayService, HolidayCalendarRepository holidayRepo) {
        this.userRepo = userRepo;
        this.configService = configService;
        this.holidayService = holidayService;
        this.holidayRepo = holidayRepo;
    }

    @Override
    public void run(String... args) {
        // 1. 管理员账号
        if (!userRepo.existsByUsername("admin")) {
            SysUser u = new SysUser();
            u.setUsername("admin");
            String pwd = System.getenv("ADMIN_PASSWORD");
            if (pwd == null || pwd.isEmpty()) pwd = "Admin@123456";
            u.setPasswordHash(PasswordUtil.encode(pwd));
            u.setRole(SysUser.ROLE_ADMIN);
            u.setStatus(1);
            u.setMustChangePwd(true);
            userRepo.save(u);
        }

        // 2. 默认配置
        configService.set(ConfigService.KEY_STD_WORK_HOURS, "7.5", "标准工时（小时/天），用于天数模式折算");
        configService.set(ConfigService.KEY_PRECISION, "2", "数值精度（小数位）");
        configService.set(ConfigService.KEY_BACKUP_RETENTION, "10", "备份保留份数");
        configService.set(ConfigService.KEY_SESSION_TIMEOUT, "24", "单设备登录会话有效期（小时）");

        // 3. 节假日（仅当该年无数据时才播种）
        int y = LocalDate.now().getYear();
        if (holidayRepo.countByHolidayDateBetween(
                LocalDate.of(y, 1, 1), LocalDate.of(y, 12, 31)) == 0) {
            holidayService.seedYear(y);
        }
        if (holidayRepo.countByHolidayDateBetween(
                LocalDate.of(y + 1, 1, 1), LocalDate.of(y + 1, 12, 31)) == 0) {
            holidayService.seedYear(y + 1);
        }
    }
}
