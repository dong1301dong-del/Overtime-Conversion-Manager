package com.lynn.overtime.controller;

import com.lynn.overtime.common.ApiResult;
import com.lynn.overtime.common.Req;
import com.lynn.overtime.entity.SysUser;
import com.lynn.overtime.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@RequestBody Map<String, Object> body) {
        String username = Req.str(body, "username");
        String password = Req.str(body, "password");
        if (username == null || password == null) {
            return ApiResult.fail("用户名和密码不能为空");
        }
        return ApiResult.ok(authService.login(username, password));
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout() {
        authService.logout();
        return ApiResult.ok();
    }

    @PostMapping("/change-password")
    public ApiResult<Map<String, Object>> changePassword(@RequestBody Map<String, Object> body) {
        String oldP = Req.str(body, "oldPassword");
        String newP = Req.str(body, "newPassword");
        return ApiResult.ok(authService.changePassword(oldP, newP));
    }

    @GetMapping("/me")
    public ApiResult<Map<String, Object>> me() {
        return ApiResult.ok(authService.me());
    }
}
