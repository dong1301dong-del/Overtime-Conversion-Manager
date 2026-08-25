package com.lynn.overtime.controller;

import com.lynn.overtime.common.ApiResult;
import com.lynn.overtime.common.Req;
import com.lynn.overtime.common.SecurityUtil;
import com.lynn.overtime.entity.Member;
import com.lynn.overtime.entity.SysUser;
import com.lynn.overtime.service.MemberService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public ApiResult<List<Member>> list(@RequestParam(required = false) String name,
                                        @RequestParam(required = false) String department) {
        SecurityUtil.require(SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK, SysUser.ROLE_READONLY);
        return ApiResult.ok(memberService.list(name, department));
    }

    @GetMapping("/{id}")
    public ApiResult<Member> get(@PathVariable Long id) {
        SecurityUtil.require(SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK, SysUser.ROLE_READONLY);
        return ApiResult.ok(memberService.get(id));
    }

    @PostMapping
    public ApiResult<Member> create(@RequestBody Map<String, Object> body) {
        SecurityUtil.require(SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK);
        String name = Req.str(body, "name");
        String username = Req.str(body, "username");
        String dept = Req.str(body, "department");
        String pwd = Req.str(body, "initPassword");
        return ApiResult.ok(memberService.create(name, username, dept, pwd));
    }

    @PutMapping("/{id}")
    public ApiResult<Member> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        SecurityUtil.require(SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK);
        return ApiResult.ok(memberService.update(id, Req.str(body, "name"), Req.str(body, "department")));
    }

    @PostMapping("/{id}/status")
    public ApiResult<Void> setStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        SecurityUtil.require(SysUser.ROLE_ADMIN);
        memberService.setStatus(id, Req.intg(body, "status"));
        return ApiResult.ok();
    }
}
