package com.lynn.overtime.controller;

import com.lynn.overtime.common.ApiResult;
import com.lynn.overtime.common.NumberUtil;
import com.lynn.overtime.common.SecurityUtil;
import com.lynn.overtime.entity.Member;
import com.lynn.overtime.entity.SysUser;
import com.lynn.overtime.service.BalanceService;
import com.lynn.overtime.service.CompUsageService;
import com.lynn.overtime.service.MemberService;
import com.lynn.overtime.service.OvertimeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final MemberService memberService;
    private final BalanceService balanceService;
    private final OvertimeService overtimeService;
    private final CompUsageService compUsageService;

    public DashboardController(MemberService memberService, BalanceService balanceService,
                               OvertimeService overtimeService, CompUsageService compUsageService) {
        this.memberService = memberService;
        this.balanceService = balanceService;
        this.overtimeService = overtimeService;
        this.compUsageService = compUsageService;
    }

    @GetMapping
    public ApiResult<Object> index() {
        SecurityUtil.require(SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK, SysUser.ROLE_READONLY);
        List<Member> members = memberService.list(null, null);
        BigDecimal totalComp = BigDecimal.ZERO;
        BigDecimal totalUsage = BigDecimal.ZERO;
        BigDecimal totalRem = BigDecimal.ZERO;
        int overdraft = 0;
        for (Member m : members) {
            Map<String, Object> b = balanceService.compute(m.getId());
            totalComp = totalComp.add((BigDecimal) b.get("compTotal"));
            totalUsage = totalUsage.add((BigDecimal) b.get("usageTotal"));
            totalRem = totalRem.add((BigDecimal) b.get("remaining"));
            if ((Boolean) b.get("overdraft")) overdraft++;
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("totalMembers", members.size());
        r.put("totalCompHours", NumberUtil.scale2(totalComp));
        r.put("totalUsageHours", NumberUtil.scale2(totalUsage));
        r.put("totalRemaining", NumberUtil.scale2(totalRem));
        r.put("overdraftCount", overdraft);
        r.put("months", overtimeService.availableMonths());
        r.put("usageByMonth", compUsageService.listAll().size());
        return ApiResult.ok(r);
    }
}
