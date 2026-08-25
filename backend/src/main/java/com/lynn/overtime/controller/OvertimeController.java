package com.lynn.overtime.controller;

import com.lynn.overtime.common.ApiResult;
import com.lynn.overtime.common.Req;
import com.lynn.overtime.common.SecurityUtil;
import com.lynn.overtime.entity.OvertimeRecord;
import com.lynn.overtime.entity.SysUser;
import com.lynn.overtime.service.OvertimeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/overtime")
public class OvertimeController {

    private final OvertimeService overtimeService;

    public OvertimeController(OvertimeService overtimeService) {
        this.overtimeService = overtimeService;
    }

    @GetMapping("/month")
    public ApiResult<Object> month(@RequestParam String month) {
        SecurityUtil.require(SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK, SysUser.ROLE_READONLY);
        return ApiResult.ok(overtimeService.monthAggregation(month));
    }

    @GetMapping("/months")
    public ApiResult<List<String>> months() {
        SecurityUtil.require(SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK, SysUser.ROLE_READONLY);
        return ApiResult.ok(overtimeService.availableMonths());
    }

    @PostMapping
    public ApiResult<OvertimeRecord> create(@RequestBody Map<String, Object> body) {
        SecurityUtil.require(SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK);
        Long memberId = Req.longg(body, "memberId");
        if (memberId == null) return ApiResult.fail("memberId 不能为空");
        OvertimeRecord r = overtimeService.saveManual(memberId, Req.date(body, "date"),
                Req.str(body, "validPeriod"), Req.str(body, "type"),
                Req.dec(body, "validHours"), Req.str(body, "punchTime"), Req.str(body, "remark"));
        return ApiResult.ok(r);
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        SecurityUtil.require(SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK);
        overtimeService.delete(id);
        return ApiResult.ok();
    }
}
