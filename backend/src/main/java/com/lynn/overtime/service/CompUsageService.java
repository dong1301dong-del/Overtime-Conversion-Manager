package com.lynn.overtime.service;

import com.lynn.overtime.common.BizException;
import com.lynn.overtime.common.DateUtil;
import com.lynn.overtime.common.NumberUtil;
import com.lynn.overtime.entity.AuditLog;
import com.lynn.overtime.entity.CompUsage;
import com.lynn.overtime.entity.Member;
import com.lynn.overtime.repository.AuditLogRepository;
import com.lynn.overtime.repository.CompUsageRepository;
import com.lynn.overtime.repository.MemberRepository;
import com.lynn.overtime.repository.OvertimeRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class CompUsageService {

    public static final int MODE_HOUR = 1;   // 模式 A：小时
    public static final int MODE_DAY = 2;    // 模式 B/C：天数（自动拆分）

    private final CompUsageRepository usageRepo;
    private final MemberRepository memberRepo;
    private final ConfigService configService;
    private final BalanceService balanceService;
    private final AuditLogRepository auditRepo;
    private final OvertimeRecordRepository overtimeRepo;

    public CompUsageService(CompUsageRepository usageRepo, MemberRepository memberRepo,
                            ConfigService configService, BalanceService balanceService,
                            AuditLogRepository auditRepo, OvertimeRecordRepository overtimeRepo) {
        this.usageRepo = usageRepo;
        this.memberRepo = memberRepo;
        this.configService = configService;
        this.balanceService = balanceService;
        this.auditRepo = auditRepo;
        this.overtimeRepo = overtimeRepo;
    }

    @Transactional
    public CompUsage saveManual(Long memberId, LocalDate useStart, LocalDate useEnd,
                                Integer mode, BigDecimal hoursOrDays, String remark) {
        Member m = memberRepo.findById(memberId).orElseThrow(() -> new BizException("成员不存在：" + memberId));
        if (mode == null || (mode != MODE_HOUR && mode != MODE_DAY)) {
            throw new BizException("模式须为 1(小时) 或 2(天数)");
        }
        CompUsage u = new CompUsage();
        u.setMemberId(m.getId());
        u.setUseStart(useStart);
        u.setUseEnd(useEnd);
        u.setMode(mode);
        u.setRemark(remark);
        u.setMonth(useStart == null ? null : DateUtil.monthOf(useStart));

        BigDecimal hours;
        if (mode == MODE_HOUR) {
            if (hoursOrDays == null || hoursOrDays.compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException("使用小时数须 ≥ 0");
            }
            hours = NumberUtil.scale2(hoursOrDays);
            u.setHours(hours);
            u.setDays(null);
        } else {
            // 天数模式：须为 0.5 的整数倍
            if (hoursOrDays == null) throw new BizException("使用天数不能为空");
            BigDecimal days = hoursOrDays;
            if (days.compareTo(BigDecimal.ZERO) <= 0) throw new BizException("使用天数须 > 0");
            BigDecimal twice = days.multiply(BigDecimal.valueOf(2));
            if (twice.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                throw new BizException("天数须为 0.5 的整数倍（如 1.0/1.5/2.0/2.5…）");
            }
            BigDecimal std = configService.stdWorkHours();
            hours = NumberUtil.scale2(days.multiply(std)); // 模式B×7.5、模式C自动拆分等价
            u.setDays(NumberUtil.scale2(days));
            u.setHours(hours);
        }

        // 透支判定：余额不足仍允许，自动标记
        BigDecimal before = balanceService.remaining(memberId);
        if (before.compareTo(hours) < 0) {
            u.setIsOverdraft(1);
            if (remark == null || remark.trim().isEmpty()) {
                u.setRemark("透支调休");
            } else if (!remark.contains("透支")) {
                u.setRemark(remark + "（透支调休）");
            }
        } else {
            u.setIsOverdraft(0);
        }

        CompUsage saved = usageRepo.save(u);
        audit("录入调休使用", "memberId=" + memberId + ", mode=" + mode + ", hours=" + hours + ", overdraft=" + u.getIsOverdraft());
        return saved;
    }

    public List<CompUsage> listByMember(Long memberId) {
        return usageRepo.findByMemberId(memberId);
    }

    public List<CompUsage> listByMonth(String month) {
        return usageRepo.findByMonth(month);
    }

    public List<CompUsage> listAll() {
        return usageRepo.findAll();
    }

    @Transactional
    public void delete(Long id) {
        usageRepo.deleteById(id);
        audit("删除调休使用", "id=" + id);
    }

    private void audit(String action, String detail) {
        com.lynn.overtime.common.AuthInfoHolder.log(auditRepo, action, detail);
    }
}
