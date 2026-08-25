package com.lynn.overtime.controller;

import com.lynn.overtime.common.ApiResult;
import com.lynn.overtime.common.SecurityUtil;
import com.lynn.overtime.entity.SysUser;
import com.lynn.overtime.service.BalanceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/balance")
public class BalanceController {

    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping("/member/{memberId}")
    public ApiResult<Object> member(@PathVariable Long memberId) {
        SecurityUtil.require(SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK, SysUser.ROLE_READONLY);
        return ApiResult.ok(balanceService.compute(memberId));
    }

    @GetMapping("/department")
    public ApiResult<List<Map<String, Object>>> department() {
        SecurityUtil.require(SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK, SysUser.ROLE_READONLY);
        return ApiResult.ok(balanceService.departmentSummary());
    }

    @GetMapping("/all")
    public ApiResult<List<Map<String, Object>>> all() {
        SecurityUtil.require(SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK, SysUser.ROLE_READONLY);
        return ApiResult.ok(balanceService.allSummary());
    }
}
