package com.lynn.overtime.service;

import com.lynn.overtime.entity.Member;
import com.lynn.overtime.entity.SysUser;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 定时任务：
 *  - 每周一 03:30 自动备份（保留 N 份）
 *  - 每月 10 号 09:00 透支提醒（向 ADMIN/CLERK 发送消息）
 *  - 每年 12-31 02:00 自动更新下一年法定节假日
 */
@Service
public class SchedulerService {

    private final BackupService backupService;
    private final BalanceService balanceService;
    private final MessageService messageService;
    private final HolidayService holidayService;
    private final MemberService memberService;

    public SchedulerService(BackupService backupService, BalanceService balanceService,
                            MessageService messageService, HolidayService holidayService,
                            MemberService memberService) {
        this.backupService = backupService;
        this.balanceService = balanceService;
        this.messageService = messageService;
        this.holidayService = holidayService;
        this.memberService = memberService;
    }

    @Scheduled(cron = "0 30 3 ? * MON")
    public void weeklyBackup() {
        try {
            backupService.backupNow();
        } catch (Exception ignored) {
        }
    }

    @Scheduled(cron = "0 0 9 10 * ?")
    public void monthlyOverdraftReminder() {
        List<Member> members = memberService.list(null, null);
        for (Member m : members) {
            BigDecimal rem = balanceService.remaining(m.getId());
            if (rem.compareTo(BigDecimal.ZERO) < 0) {
                messageService.sendToRoles("OVERDRAFT",
                        "【透支提醒】成员 " + m.getName() + " 调休余额已透支 " + rem.abs() + " 小时，请关注。",
                        2, SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK);
            }
        }
    }

    @Scheduled(cron = "0 0 2 31 12 ?")
    public void yearEndHolidayUpdate() {
        try {
            holidayService.autoUpdateNextYear();
        } catch (Exception ignored) {
        }
    }
}
