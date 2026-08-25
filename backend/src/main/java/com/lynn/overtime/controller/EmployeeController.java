package com.lynn.overtime.controller;

import com.lynn.overtime.common.ApiResult;
import com.lynn.overtime.common.DateUtil;
import com.lynn.overtime.common.SecurityUtil;
import com.lynn.overtime.entity.SysUser;
import com.lynn.overtime.service.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/employee")
public class EmployeeController {

    private final MemberService memberService;
    private final BalanceService balanceService;
    private final OvertimeService overtimeService;
    private final CompUsageService compUsageService;
    private final CompAdjustmentService adjustmentService;

    public EmployeeController(MemberService memberService, BalanceService balanceService,
                              OvertimeService overtimeService, CompUsageService compUsageService,
                              CompAdjustmentService adjustmentService) {
        this.memberService = memberService;
        this.balanceService = balanceService;
        this.overtimeService = overtimeService;
        this.compUsageService = compUsageService;
        this.adjustmentService = adjustmentService;
    }

    @GetMapping("/me")
    public ApiResult<Object> me(@RequestParam(required = false) String month) {
        SecurityUtil.require(SysUser.ROLE_EMPLOYEE);
        Long memberId = SecurityUtil.currentMemberId();
        if (month == null || month.isEmpty()) {
            month = DateUtil.monthOf(java.time.LocalDate.now());
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("info", memberService.get(memberId));
        r.put("balance", balanceService.compute(memberId));
        r.put("overtime", overtimeService.listByMemberMonth(memberId, month));
        r.put("usage", compUsageService.listByMember(memberId));
        r.put("adjustment", adjustmentService.listByMember(memberId));
        return ApiResult.ok(r);
    }
}
