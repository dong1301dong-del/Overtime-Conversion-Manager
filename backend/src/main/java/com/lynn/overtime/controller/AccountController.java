package com.lynn.overtime.controller;

import com.lynn.overtime.common.ApiResult;
import com.lynn.overtime.common.Req;
import com.lynn.overtime.common.SecurityUtil;
import com.lynn.overtime.entity.SysUser;
import com.lynn.overtime.service.AccountService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ApiResult<List<SysUser>> list() {
        SecurityUtil.require(SysUser.ROLE_ADMIN);
        return ApiResult.ok(accountService.list());
    }

    @PostMapping
    public ApiResult<SysUser> create(@RequestBody Map<String, Object> body) {
        SecurityUtil.require(SysUser.ROLE_ADMIN);
        Boolean must = Req.bool(body, "mustChangePwd");
        SysUser u = accountService.create(Req.str(body, "username"), Req.str(body, "role"),
                Req.str(body, "password"), Boolean.TRUE.equals(must));
        return ApiResult.ok(u);
    }

    @PostMapping("/{id}/status")
    public ApiResult<Void> setStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        SecurityUtil.require(SysUser.ROLE_ADMIN);
        accountService.setStatus(id, Req.intg(body, "status"));
        return ApiResult.ok();
    }

    @PostMapping("/{id}/reset-password")
    public ApiResult<Void> reset(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        SecurityUtil.require(SysUser.ROLE_ADMIN);
        accountService.resetPassword(id, Req.str(body, "newPassword"));
        return ApiResult.ok();
    }
}
