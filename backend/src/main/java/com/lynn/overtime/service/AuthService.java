package com.lynn.overtime.service;

import com.lynn.overtime.common.AuthInfoHolder;
import com.lynn.overtime.common.BizException;
import com.lynn.overtime.common.CurrentUserHolder;
import com.lynn.overtime.common.PasswordUtil;
import com.lynn.overtime.entity.AuditLog;
import com.lynn.overtime.entity.Member;
import com.lynn.overtime.entity.SysUser;
import com.lynn.overtime.repository.AuditLogRepository;
import com.lynn.overtime.repository.MemberRepository;
import com.lynn.overtime.repository.SysUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final SysUserRepository userRepo;
    private final MemberRepository memberRepo;
    private final ConfigService configService;
    private final AuditLogRepository auditRepo;

    public AuthService(SysUserRepository userRepo, MemberRepository memberRepo,
                       ConfigService configService, AuditLogRepository auditRepo) {
        this.userRepo = userRepo;
        this.memberRepo = memberRepo;
        this.configService = configService;
        this.auditRepo = auditRepo;
    }

    @Transactional
    public Map<String, Object> login(String username, String password) {
        SysUser u = userRepo.findByUsername(username).orElseThrow(() -> new BizException("用户名或密码错误"));
        if (!PasswordUtil.matches(password, u.getPasswordHash())) {
            throw new BizException("用户名或密码错误");
        }
        if (u.getStatus() != null && u.getStatus() == 0) {
            throw new BizException("账号已被禁用，请联系管理员");
        }
        // 单设备：生成新 token，覆盖旧会话
        String token = UUID.randomUUID().toString().replace("-", "");
        u.setSessionToken(token);
        u.setSessionExpireAt(LocalDateTime.now().plusHours(configService.sessionTimeoutHours()));
        u.setLastLoginAt(LocalDateTime.now());
        userRepo.save(u);

        Map<String, Object> result = buildUserInfo(u);
        AuthInfoHolder.log(auditRepo, "登录", "username=" + username);
        return result;
    }

    @Transactional
    public void logout() {
        com.lynn.overtime.common.AuthInfo info = CurrentUserHolder.get();
        if (info == null) return;
        userRepo.findById(info.getUserId()).ifPresent(u -> {
            u.setSessionToken(null);
            u.setSessionExpireAt(null);
            userRepo.save(u);
        });
        AuthInfoHolder.log(auditRepo, "登出", "");
    }

    @Transactional
    public Map<String, Object> changePassword(String oldPassword, String newPassword) {
        com.lynn.overtime.common.AuthInfo info = CurrentUserHolder.get();
        if (info == null) throw new BizException("未登录");
        SysUser u = userRepo.findById(info.getUserId()).orElseThrow(() -> new BizException("账号不存在"));
        if (!PasswordUtil.matches(oldPassword, u.getPasswordHash())) {
            throw new BizException("原密码错误");
        }
        if (!PasswordUtil.isStrong(newPassword)) {
            throw new BizException("新密码强度不足：长度8-20，含大写+小写+数字+特殊字符");
        }
        if (PasswordUtil.sameAsUsername(newPassword, u.getUsername())) {
            throw new BizException("新密码不能与用户名相同");
        }
        u.setPasswordHash(PasswordUtil.encode(newPassword));
        u.setMustChangePwd(false);
        // 重新签发会话，保持登录
        String token = UUID.randomUUID().toString().replace("-", "");
        u.setSessionToken(token);
        u.setSessionExpireAt(LocalDateTime.now().plusHours(configService.sessionTimeoutHours()));
        userRepo.save(u);
        AuthInfoHolder.log(auditRepo, "修改密码", "username=" + u.getUsername());

        Map<String, Object> result = buildUserInfo(u);
        return result;
    }

    public Map<String, Object> me() {
        com.lynn.overtime.common.AuthInfo info = CurrentUserHolder.get();
        if (info == null) throw new BizException("未登录");
        SysUser u = userRepo.findById(info.getUserId()).orElseThrow(() -> new BizException("账号不存在"));
        return buildUserInfo(u);
    }

    private Map<String, Object> buildUserInfo(SysUser u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("token", u.getSessionToken());
        m.put("username", u.getUsername());
        m.put("role", u.getRole());
        m.put("memberId", u.getMemberId());
        m.put("mustChangePwd", u.getMustChangePwd());
        m.put("status", u.getStatus());
        if (u.getMemberId() != null) {
            Optional<Member> mem = memberRepo.findById(u.getMemberId());
            m.put("name", mem.map(Member::getName).orElse(u.getUsername()));
            m.put("department", mem.map(Member::getDepartment).orElse(null));
        } else {
            m.put("name", u.getUsername());
        }
        return m;
    }
}
