package com.lynn.overtime.service;

import com.lynn.overtime.common.BizException;
import com.lynn.overtime.common.CurrentUserHolder;
import com.lynn.overtime.common.DateUtil;
import com.lynn.overtime.common.NumberUtil;
import com.lynn.overtime.entity.AuditLog;
import com.lynn.overtime.entity.Member;
import com.lynn.overtime.entity.OvertimeRecord;
import com.lynn.overtime.repository.AuditLogRepository;
import com.lynn.overtime.repository.MemberRepository;
import com.lynn.overtime.repository.OvertimeRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OvertimeService {

    private final OvertimeRecordRepository overtimeRepo;
    private final MemberRepository memberRepo;
    private final HolidayService holidayService;
    private final AuditLogRepository auditRepo;

    public OvertimeService(OvertimeRecordRepository overtimeRepo, MemberRepository memberRepo,
                           HolidayService holidayService, AuditLogRepository auditRepo) {
        this.overtimeRepo = overtimeRepo;
        this.memberRepo = memberRepo;
        this.holidayService = holidayService;
        this.auditRepo = auditRepo;
    }

    /** 由日期与（可选）类型覆盖，计算类型/比例/折算时长，并填充记录字段 */
    public OvertimeRecord buildRecord(Long memberId, LocalDate date, String validPeriod,
                                      String typeOverride, BigDecimal validHours) {
        Member m = memberRepo.findById(memberId).orElseThrow(() -> new BizException("成员不存在：" + memberId));
        if (validHours == null || validHours.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("有效加班时长须 ≥ 0");
        }
        String type;
        BigDecimal ratio;
        if (typeOverride != null && !typeOverride.isEmpty()) {
            if (!List.of("工作日", "周末", "法定节假日").contains(typeOverride)) {
                throw new BizException("类型须为 工作日/周末/法定节假日");
            }
            type = typeOverride;
            ratio = holidayService.ratioOfType(type);
        } else {
            HolidayService.TypeResult tr = holidayService.determineType(date);
            type = tr.type;
            ratio = tr.ratio;
        }
        OvertimeRecord r = new OvertimeRecord();
        r.setMemberId(m.getId());
        r.setOvertimeDate(date);
        r.setWeekday(DateUtil.weekdayCN(date));
        r.setValidPeriod(validPeriod);
        r.setType(type);
        r.setRatio(NumberUtil.scale2(ratio));
        r.setValidHours(NumberUtil.scale2(validHours));
        r.setCompHours(NumberUtil.scale2(validHours.multiply(ratio)));
        r.setMonth(DateUtil.monthOf(date));
        return r;
    }

    @Transactional
    public OvertimeRecord saveManual(Long memberId, LocalDate date, String validPeriod,
                                     String typeOverride, BigDecimal validHours,
                                     String punchTime, String remark) {
        OvertimeRecord r = buildRecord(memberId, date, validPeriod, typeOverride, validHours);
        r.setPunchTime(punchTime);
        r.setRemark(remark);
        OvertimeRecord saved = overtimeRepo.save(r);
        audit("录入加班记录", "memberId=" + memberId + ", date=" + date + ", compHours=" + saved.getCompHours());
        return saved;
    }

    public List<OvertimeRecord> listByMonth(String month) {
        return overtimeRepo.findByMonth(month);
    }

    public List<OvertimeRecord> listByMemberMonth(Long memberId, String month) {
        return overtimeRepo.findByMemberIdAndMonth(memberId, month);
    }

    public List<OvertimeRecord> listByMember(Long memberId) {
        return overtimeRepo.findByMemberId(memberId);
    }

    /** 已有数据的月份列表（倒序），用于首页/下拉 */
    public List<String> availableMonths() {
        return overtimeRepo.findDistinctMonths();
    }

    /** 月度全员详表聚合 */
    public Map<String, Object> monthAggregation(String month) {
        List<OvertimeRecord> list = overtimeRepo.findByMonth(month);
        BigDecimal totalValid = BigDecimal.ZERO;
        BigDecimal totalComp = BigDecimal.ZERO;
        for (OvertimeRecord r : list) {
            totalValid = totalValid.add(r.getValidHours() == null ? BigDecimal.ZERO : r.getValidHours());
            totalComp = totalComp.add(r.getCompHours() == null ? BigDecimal.ZERO : r.getCompHours());
        }
        Map<String, Object> agg = new LinkedHashMap<>();
        agg.put("month", month);
        agg.put("count", list.size());
        agg.put("totalValidHours", NumberUtil.scale2(totalValid));
        agg.put("totalCompHours", NumberUtil.scale2(totalComp));
        agg.put("records", list);
        return agg;
    }

    @Transactional
    public void delete(Long id) {
        overtimeRepo.deleteById(id);
        audit("删除加班记录", "id=" + id);
    }

    private void audit(String action, String detail) {
        com.lynn.overtime.common.AuthInfoHolder.log(auditRepo, action, detail);
    }
}
