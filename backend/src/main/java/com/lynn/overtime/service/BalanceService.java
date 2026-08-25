package com.lynn.overtime.service;

import com.lynn.overtime.common.NumberUtil;
import com.lynn.overtime.entity.Member;
import com.lynn.overtime.repository.CompAdjustmentRepository;
import com.lynn.overtime.repository.CompUsageRepository;
import com.lynn.overtime.repository.MemberRepository;
import com.lynn.overtime.repository.OvertimeRecordRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BalanceService {

    private final OvertimeRecordRepository overtimeRepo;
    private final CompUsageRepository usageRepo;
    private final CompAdjustmentRepository adjustRepo;
    private final MemberRepository memberRepo;

    public BalanceService(OvertimeRecordRepository overtimeRepo, CompUsageRepository usageRepo,
                          CompAdjustmentRepository adjustRepo, MemberRepository memberRepo) {
        this.overtimeRepo = overtimeRepo;
        this.usageRepo = usageRepo;
        this.adjustRepo = adjustRepo;
        this.memberRepo = memberRepo;
    }

    public Map<String, Object> compute(Long memberId) {
        BigDecimal comp = nz(overtimeRepo.sumCompHoursByMember(memberId));
        BigDecimal adjust = nz(adjustRepo.sumHoursByMember(memberId));
        BigDecimal usage = nz(usageRepo.sumHoursByMember(memberId));
        BigDecimal remaining = NumberUtil.scale2(comp.add(adjust).subtract(usage));
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("memberId", memberId);
        r.put("compTotal", NumberUtil.scale2(comp));
        r.put("adjustTotal", NumberUtil.scale2(adjust));
        r.put("usageTotal", NumberUtil.scale2(usage));
        r.put("remaining", remaining);
        r.put("overdraft", remaining.compareTo(BigDecimal.ZERO) < 0);
        return r;
    }

    public BigDecimal remaining(Long memberId) {
        Map<String, Object> m = compute(memberId);
        return (BigDecimal) m.get("remaining");
    }

    /** 全员余额汇总（M4 剩余查询-全部成员视角） */
    public List<Map<String, Object>> allSummary() {
        List<Map<String, Object>> res = new ArrayList<>();
        for (Member m : memberRepo.findAll()) {
            Map<String, Object> b = compute(m.getId());
            b.put("memberId", m.getId());
            b.put("name", m.getName());
            b.put("department", m.getDepartment());
            b.put("status", m.getStatus());
            res.add(b);
        }
        return res;
    }

    /** 部门维度汇总（M4 剩余查询-部门卡片） */
    public List<Map<String, Object>> departmentSummary() {
        Map<String, Map<String, Object>> map = new LinkedHashMap<>();
        for (Member m : memberRepo.findAll()) {
            Map<String, Object> b = compute(m.getId());
            String dept = (m.getDepartment() == null || m.getDepartment().isEmpty()) ? "未分配" : m.getDepartment();
            Map<String, Object> agg = map.computeIfAbsent(dept, k -> {
                Map<String, Object> x = new LinkedHashMap<>();
                x.put("department", k);
                x.put("memberCount", 0);
                x.put("compTotal", BigDecimal.ZERO);
                x.put("adjustTotal", BigDecimal.ZERO);
                x.put("usageTotal", BigDecimal.ZERO);
                x.put("remaining", BigDecimal.ZERO);
                x.put("overdraftCount", 0);
                return x;
            });
            agg.put("memberCount", (Integer) agg.get("memberCount") + 1);
            agg.put("compTotal", NumberUtil.scale2(((BigDecimal) agg.get("compTotal")).add((BigDecimal) b.get("compTotal"))));
            agg.put("adjustTotal", NumberUtil.scale2(((BigDecimal) agg.get("adjustTotal")).add((BigDecimal) b.get("adjustTotal"))));
            agg.put("usageTotal", NumberUtil.scale2(((BigDecimal) agg.get("usageTotal")).add((BigDecimal) b.get("usageTotal"))));
            agg.put("remaining", NumberUtil.scale2(((BigDecimal) agg.get("remaining")).add((BigDecimal) b.get("remaining"))));
            if ((Boolean) b.get("overdraft")) {
                agg.put("overdraftCount", (Integer) agg.get("overdraftCount") + 1);
            }
        }
        return new ArrayList<>(map.values());
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
