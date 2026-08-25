package com.lynn.overtime.controller;

import com.lynn.overtime.common.ApiResult;
import com.lynn.overtime.common.Req;
import com.lynn.overtime.common.SecurityUtil;
import com.lynn.overtime.entity.*;
import com.lynn.overtime.repository.BackupLogRepository;
import com.lynn.overtime.service.BackupService;
import com.lynn.overtime.service.ConfigService;
import com.lynn.overtime.service.HolidayService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final ConfigService configService;
    private final HolidayService holidayService;
    private final BackupService backupService;
    private final BackupLogRepository backupLogRepo;

    public ConfigController(ConfigService configService, HolidayService holidayService,
                            BackupService backupService, BackupLogRepository backupLogRepo) {
        this.configService = configService;
        this.holidayService = holidayService;
        this.backupService = backupService;
        this.backupLogRepo = backupLogRepo;
    }

    @GetMapping
    public ApiResult<List<SysConfig>> list() {
        SecurityUtil.require(SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK, SysUser.ROLE_READONLY);
        return ApiResult.ok(configService.listAll());
    }

    @PostMapping
    public ApiResult<Void> set(@RequestBody Map<String, Object> body) {
        SecurityUtil.require(SysUser.ROLE_ADMIN);
        configService.set(Req.str(body, "key"), Req.str(body, "value"), Req.str(body, "description"));
        return ApiResult.ok();
    }

    @GetMapping("/holidays")
    public ApiResult<List<HolidayCalendar>> holidays(@RequestParam int year) {
        SecurityUtil.require(SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK, SysUser.ROLE_READONLY);
        return ApiResult.ok(holidayService.listYear(year));
    }

    @PostMapping("/holidays")
    public ApiResult<HolidayCalendar> addHoliday(@RequestBody Map<String, Object> body) {
        SecurityUtil.require(SysUser.ROLE_ADMIN);
        return ApiResult.ok(holidayService.saveManual(Req.date(body, "holidayDate"),
                Req.str(body, "name"), Req.str(body, "kind")));
    }

    @DeleteMapping("/holidays/{id}")
    public ApiResult<Void> delHoliday(@PathVariable Long id) {
        SecurityUtil.require(SysUser.ROLE_ADMIN);
        holidayService.delete(id);
        return ApiResult.ok();
    }

    @PostMapping("/holidays/seed")
    public ApiResult<Void> seed(@RequestParam int year) {
        SecurityUtil.require(SysUser.ROLE_ADMIN);
        holidayService.seedYear(year);
        return ApiResult.ok();
    }

    @PostMapping("/backup")
    public ApiResult<Object> backup() {
        SecurityUtil.require(SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK);
        return ApiResult.ok(backupService.backupNow());
    }

    @GetMapping("/backups")
    public ApiResult<List<BackupLog>> backups() {
        SecurityUtil.require(SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK);
        return ApiResult.ok(backupLogRepo.findTop20ByOrderByCreatedAtDesc());
    }
}
