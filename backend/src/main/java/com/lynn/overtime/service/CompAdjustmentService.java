package com.lynn.overtime.service;

import com.lynn.overtime.common.BizException;
import com.lynn.overtime.common.DateUtil;
import com.lynn.overtime.common.NumberUtil;
import com.lynn.overtime.entity.AuditLog;
import com.lynn.overtime.entity.CompAdjustment;
import com.lynn.overtime.entity.Member;
import com.lynn.overtime.repository.AuditLogRepository;
import com.lynn.overtime.repository.CompAdjustmentRepository;
import com.lynn.overtime.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class CompAdjustmentService {

    public static final List<String> CATEGORIES = List.of("项目奖励", "领导奖励", "公司福利", "其他");

    private final CompAdjustmentRepository adjustRepo;
    private final MemberRepository memberRepo;
    private final AuditLogRepository auditRepo;

    public CompAdjustmentService(CompAdjustmentRepository adjustRepo, MemberRepository memberRepo, AuditLogRepository auditRepo) {
        this.adjustRepo = adjustRepo;
        this.memberRepo = memberRepo;
        this.auditRepo = auditRepo;
    }

    @Transactional
    public CompAdjustment saveManual(Long memberId, LocalDate date, BigDecimal hours, String category, String remark) {
        Member m = memberRepo.findById(memberId).orElseThrow(() -> new BizException("成员不存在：" + memberId));
        if (hours == null || hours.compareTo(BigDecimal.ZERO) < 0) throw new BizException("其他调休时长须 ≥ 0");
        if (!CATEGORIES.contains(category)) throw new BizException("来源类别须为：" + String.join("/", CATEGORIES));
        CompAdjustment a = new CompAdjustment();
        a.setMemberId(m.getId());
        a.setDate(date);
        a.setHours(NumberUtil.scale2(hours));
        a.setCategory(category);
        a.setRemark(remark);
        a.setMonth(date == null ? null : DateUtil.monthOf(date));
        CompAdjustment saved = adjustRepo.save(a);
        audit("录入其他调休", "memberId=" + memberId + ", category=" + category + ", hours=" + saved.getHours());
        return saved;
    }

    public List<CompAdjustment> listByMember(Long memberId) {
        return adjustRepo.findByMemberId(memberId);
    }

    @Transactional
    public void delete(Long id) {
        adjustRepo.deleteById(id);
        audit("删除其他调休", "id=" + id);
    }

    private void audit(String action, String detail) {
        com.lynn.overtime.common.AuthInfoHolder.log(auditRepo, action, detail);
    }
}
