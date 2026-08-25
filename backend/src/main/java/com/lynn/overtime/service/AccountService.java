package com.lynn.overtime.service;

import com.lynn.overtime.common.AuthInfoHolder;
import com.lynn.overtime.common.BizException;
import com.lynn.overtime.common.CurrentUserHolder;
import com.lynn.overtime.common.PasswordUtil;
import com.lynn.overtime.entity.AuditLog;
import com.lynn.overtime.entity.SysUser;
import com.lynn.overtime.repository.AuditLogRepository;
import com.lynn.overtime.repository.SysUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountService {

    private static final List<String> MANAGEABLE_ROLES = List.of(
            SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK, SysUser.ROLE_READONLY);

    private final SysUserRepository userRepo;
    private final AuditLogRepository auditRepo;

    public AccountService(SysUserRepository userRepo, AuditLogRepository auditRepo) {
        this.userRepo = userRepo;
        this.auditRepo = auditRepo;
    }

    @Transactional
    public SysUser create(String username, String role, String password, boolean mustChangePwd) {
        if (!MemberService.isValidUsername(username)) throw new BizException("用户名须为字母与数字组成，最长26位");
        if (!MANAGEABLE_ROLES.contains(role)) throw new BizException("角色须为 ADMIN/CLERK/READONLY");
        if (userRepo.existsByUsername(username)) throw new BizException("账号已存在：" + username);
        if (!PasswordUtil.isStrong(password)) throw new BizException("密码强度不足：长度8-20，含大写+小写+数字+特殊字符");
        if (PasswordUtil.sameAsUsername(password, username)) throw new BizException("密码不能与用户名相同");

        SysUser u = new SysUser();
        u.setUsername(username);
        u.setPasswordHash(PasswordUtil.encode(password));
        u.setRole(role);
        u.setMemberId(null);
        u.setStatus(1);
        u.setMustChangePwd(mustChangePwd);
        SysUser saved = userRepo.save(u);
        audit("创建账号", "username=" + username + ", role=" + role);
        return saved;
    }

    public List<SysUser> list() {
        return userRepo.findAll();
    }

    @Transactional
    public void setStatus(Long id, int status) {
        SysUser u = userRepo.findById(id).orElseThrow(() -> new BizException("账号不存在"));
        u.setStatus(status);
        // 失效其会话
        u.setSessionToken(null);
        u.setSessionExpireAt(null);
        userRepo.save(u);
        audit(status == 1 ? "解禁账号" : "禁用账号", "username=" + u.getUsername());
    }

    @Transactional
    public void resetPassword(Long targetId, String newPassword) {
        SysUser u = userRepo.findById(targetId).orElseThrow(() -> new BizException("账号不存在"));
        if (!PasswordUtil.isStrong(newPassword)) throw new BizException("密码强度不足");
        if (PasswordUtil.sameAsUsername(newPassword, u.getUsername())) throw new BizException("密码不能与用户名相同");
        u.setPasswordHash(PasswordUtil.encode(newPassword));
        u.setMustChangePwd(true);
        u.setSessionToken(null); // 强制重新登录
        userRepo.save(u);
        audit("重置密码", "target=" + u.getUsername());
    }

    public SysUser get(Long id) {
        return userRepo.findById(id).orElseThrow(() -> new BizException("账号不存在"));
    }

    private void audit(String action, String detail) {
        AuthInfoHolder.log(auditRepo, action, detail);
    }
}
