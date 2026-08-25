package com.lynn.overtime.controller;

import com.lynn.overtime.common.ApiResult;
import com.lynn.overtime.common.Req;
import com.lynn.overtime.common.SecurityUtil;
import com.lynn.overtime.entity.CompAdjustment;
import com.lynn.overtime.entity.SysUser;
import com.lynn.overtime.service.CompAdjustmentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/adjustment")
public class AdjustmentController {

    private final CompAdjustmentService adjustmentService;

    public AdjustmentController(CompAdjustmentService adjustmentService) {
        this.adjustmentService = adjustmentService;
    }

    @GetMapping
    public ApiResult<List<CompAdjustment>> list(@RequestParam Long memberId) {
        SecurityUtil.require(SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK, SysUser.ROLE_READONLY);
        return ApiResult.ok(adjustmentService.listByMember(memberId));
    }

    @PostMapping
    public ApiResult<CompAdjustment> create(@RequestBody Map<String, Object> body) {
        SecurityUtil.require(SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK);
        Long memberId = Req.longg(body, "memberId");
        if (memberId == null) return ApiResult.fail("memberId 不能为空");
        CompAdjustment a = adjustmentService.saveManual(memberId, Req.date(body, "date"),
                Req.dec(body, "hours"), Req.str(body, "category"), Req.str(body, "remark"));
        return ApiResult.ok(a);
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        SecurityUtil.require(SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK);
        adjustmentService.delete(id);
        return ApiResult.ok();
    }
}
