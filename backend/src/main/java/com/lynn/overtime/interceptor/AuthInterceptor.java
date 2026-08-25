package com.lynn.overtime.interceptor;

import com.lynn.overtime.common.ApiResult;
import com.lynn.overtime.common.AuthInfo;
import com.lynn.overtime.common.CurrentUserHolder;
import com.lynn.overtime.entity.SysUser;
import com.lynn.overtime.repository.SysUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final SysUserRepository userRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthInterceptor(SysUserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        // 仅匿名接口（登录、注销）放行；其余 /api/auth/*（如改密码、/me）仍需鉴权
        if ("/api/auth/login".equals(uri) || "/api/auth/logout".equals(uri)) {
            return true;
        }

        String token = request.getHeader("X-Auth-Token");
        if (token == null) {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.toLowerCase().startsWith("bearer ")) {
                token = auth.substring(7).trim();
            }
        }

        if (token == null || token.isEmpty()) {
            writeUnauthorized(response, "未登录或会话已失效");
            return false;
        }

        Optional<SysUser> opt = userRepo.findBySessionToken(token);
        if (opt.isEmpty()) {
            writeUnauthorized(response, "账号已在其他设备登录，您已退出");
            return false;
        }
        SysUser u = opt.get();
        if (u.getStatus() != null && u.getStatus() == 0) {
            writeUnauthorized(response, "账号已被禁用");
            return false;
        }
        if (u.getSessionExpireAt() != null && u.getSessionExpireAt().isBefore(LocalDateTime.now())) {
            writeUnauthorized(response, "会话已过期，请重新登录");
            return false;
        }

        CurrentUserHolder.set(new AuthInfo(u.getId(), u.getUsername(), u.getRole(), u.getMemberId()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserHolder.clear();
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK); // 业务层用 code 表达，便于前端统一处理
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiResult<Void> r = ApiResult.fail(401, msg);
        response.getWriter().write(objectMapper.writeValueAsString(r));
    }
}
