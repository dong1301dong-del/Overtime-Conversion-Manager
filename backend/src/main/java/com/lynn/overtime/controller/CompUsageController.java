package com.lynn.overtime.controller;

import com.lynn.overtime.common.ApiResult;
import com.lynn.overtime.common.Req;
import com.lynn.overtime.common.SecurityUtil;
import com.lynn.overtime.entity.CompUsage;
import com.lynn.overtime.entity.SysUser;
import com.lynn.overtime.service.CompUsageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comp-usage")
public class CompUsageController {

    private final CompUsageService compUsageService;

    public CompUsageController(CompUsageService compUsageService) {
        this.compUsageService = compUsageService;
    }

    @GetMapping
    public ApiResult<List<CompUsage>> list(@RequestParam(required = false) Long memberId,
                                           @RequestParam(required = false) String month) {
        SecurityUtil.require(SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK, SysUser.ROLE_READONLY);
        if (memberId != null) return ApiResult.ok(compUsageService.listByMember(memberId));
        if (month != null) return ApiResult.ok(compUsageService.listByMonth(month));
        return ApiResult.ok(compUsageService.listAll());
    }

    @PostMapping
    public ApiResult<CompUsage> create(@RequestBody Map<String, Object> body) {
        SecurityUtil.require(SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK);
        Long memberId = Req.longg(body, "memberId");
        if (memberId == null) return ApiResult.fail("memberId 不能为空");
        Integer mode = Req.intg(body, "mode");
        CompUsage u = compUsageService.saveManual(memberId, Req.date(body, "useStart"),
                Req.date(body, "useEnd"), mode, Req.dec(body, "hoursOrDays"), Req.str(body, "remark"));
        return ApiResult.ok(u);
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        SecurityUtil.require(SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK);
        compUsageService.delete(id);
        return ApiResult.ok();
    }
}
