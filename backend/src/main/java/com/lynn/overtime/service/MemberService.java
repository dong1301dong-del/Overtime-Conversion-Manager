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

import java.util.List;
import java.util.regex.Pattern;

@Service
public class MemberService {

    public static final String DEFAULT_EMP_PWD = "Abc_123456";
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9]{1,26}$");

    private final MemberRepository memberRepo;
    private final SysUserRepository userRepo;
    private final AuditLogRepository auditRepo;

    public MemberService(MemberRepository memberRepo, SysUserRepository userRepo, AuditLogRepository auditRepo) {
        this.memberRepo = memberRepo;
        this.userRepo = userRepo;
        this.auditRepo = auditRepo;
    }

    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    @Transactional
    public Member create(String name, String username, String department, String initPassword) {
        if (name == null || name.trim().isEmpty()) throw new BizException("姓名不能为空");
        if (!isValidUsername(username)) throw new BizException("用户名须为字母与数字组成，最长26位，不允许中文");
        if (memberRepo.existsByUsername(username)) throw new BizException("用户名已存在：" + username);
        if (userRepo.existsByUsername(username)) throw new BizException("系统账号已存在：" + username);

        Member m = new Member();
        m.setName(name.trim());
        m.setUsername(username);
        m.setDepartment(department);
        m.setStatus(1);
        Member saved = memberRepo.save(m);

        // 自动创建员工自助查询账号
        SysUser u = new SysUser();
        u.setUsername(username);
        u.setPasswordHash(PasswordUtil.encode(initPassword == null || initPassword.isEmpty() ? DEFAULT_EMP_PWD : initPassword));
        u.setRole(SysUser.ROLE_EMPLOYEE);
        u.setMemberId(saved.getId());
        u.setStatus(1);
        u.setMustChangePwd(true);
        userRepo.save(u);

        audit("新增成员", "name=" + name + ", username=" + username + ", dept=" + department);
        return saved;
    }

    @Transactional
    public Member update(Long id, String name, String department) {
        Member m = memberRepo.findById(id).orElseThrow(() -> new BizException("成员不存在"));
        if (name != null && !name.trim().isEmpty()) m.setName(name.trim());
        if (department != null) m.setDepartment(department);
        Member saved = memberRepo.save(m);
        audit("修改成员", "id=" + id);
        return saved;
    }

    @Transactional
    public void setStatus(Long id, int status) {
        Member m = memberRepo.findById(id).orElseThrow(() -> new BizException("成员不存在"));
        m.setStatus(status);
        memberRepo.save(m);
        // 同步禁用/启用对应员工账号
        userRepo.findByMemberId(id).ifPresent(u -> {
            u.setStatus(status);
            userRepo.save(u);
        });
        audit(status == 1 ? "解禁成员" : "禁用成员", "id=" + id);
    }

    public List<Member> list(String name, String department) {
        if ((name == null || name.isEmpty()) && (department == null || department.isEmpty())) {
            return memberRepo.findAll();
        }
        // 简单包含匹配
        String n = name == null ? "" : name;
        String d = department == null ? "" : department;
        return memberRepo.findByNameContainingAndDepartmentContainingAndIdIsNotNull(n, d);
    }

    public Member get(Long id) {
        return memberRepo.findById(id).orElseThrow(() -> new BizException("成员不存在"));
    }

    private void audit(String action, String detail) {
        AuthInfoHolder.log(auditRepo, action, detail);
    }
}
