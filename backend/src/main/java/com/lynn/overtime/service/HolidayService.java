package com.lynn.overtime.service;

import com.lynn.overtime.common.NumberUtil;
import com.lynn.overtime.entity.HolidayCalendar;
import com.lynn.overtime.repository.HolidayCalendarRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class HolidayService {

    public static class TypeResult {
        public String type;
        public BigDecimal ratio;
    }

    private final HolidayCalendarRepository holidayRepo;

    public HolidayService(HolidayCalendarRepository holidayRepo) {
        this.holidayRepo = holidayRepo;
    }

    /**
     * 折算类型判定（优先级：①手动标记 > ②节假日日历 > ③默认周末/工作日）
     */
    public TypeResult determineType(LocalDate date) {
        Optional<HolidayCalendar> cal = holidayRepo.findByHolidayDate(date);
        if (cal.isPresent()) {
            if (HolidayCalendar.KIND_WORKDAY.equals(cal.get().getKind())) {
                return result("工作日", NumberUtil.of("0.5"));
            }
            return result("法定节假日", NumberUtil.of("1"));
        }
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return result("周末", NumberUtil.of("1"));
        }
        return result("工作日", NumberUtil.of("0.5"));
    }

    /** 录入员/系统可覆盖：给定类型反推比例 */
    public BigDecimal ratioOfType(String type) {
        if ("周末".equals(type) || "法定节假日".equals(type)) return NumberUtil.of("1");
        return NumberUtil.of("0.5");
    }

    /**
     * 年度自动更新：清空该年"自动"条目后重新播种（保留手动条目）。
     * 注意：内置节假日为对照国务院发布的近似值，请管理员在年底校核后手动修正。
     */
    public void seedYear(int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        holidayRepo.findByHolidayDateBetween(start, end).forEach(h -> {
            if (h.getAuto() != null && h.getAuto() == 1) holidayRepo.delete(h);
        });
        for (LocalDate d : spansOf(year)) {
            if (holidayRepo.findByHolidayDate(d).isEmpty()) {
                HolidayCalendar h = new HolidayCalendar();
                h.setHolidayDate(d);
                h.setName("法定节假日");
                h.setKind(HolidayCalendar.KIND_HOLIDAY);
                h.setAuto(1);
                holidayRepo.save(h);
            }
        }
    }

    /** 每年 12-31 调用：播种下一年 */
    public void autoUpdateNextYear() {
        seedYear(LocalDate.now().getYear() + 1);
    }

    /** 某年全部节假日（按日期升序） */
    public List<HolidayCalendar> listYear(int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        List<HolidayCalendar> list = holidayRepo.findByHolidayDateBetween(start, end);
        list.sort((a, b) -> a.getHolidayDate().compareTo(b.getHolidayDate()));
        return list;
    }

    @org.springframework.transaction.annotation.Transactional
    public HolidayCalendar saveManual(LocalDate date, String name, String kind) {
        if (date == null) throw new com.lynn.overtime.common.BizException("日期不能为空");
        if (!List.of(HolidayCalendar.KIND_HOLIDAY, HolidayCalendar.KIND_WORKDAY).contains(kind)) {
            throw new com.lynn.overtime.common.BizException("类型须为 holiday/workday");
        }
        HolidayCalendar h = holidayRepo.findByHolidayDate(date).orElse(new HolidayCalendar());
        h.setHolidayDate(date);
        h.setName(name == null || name.isEmpty() ? "法定节假日" : name);
        h.setKind(kind);
        h.setAuto(0); // 手动维护
        return holidayRepo.save(h);
    }

    @org.springframework.transaction.annotation.Transactional
    public void delete(Long id) {
        holidayRepo.deleteById(id);
    }

    private TypeResult result(String type, BigDecimal ratio) {
        TypeResult r = new TypeResult();
        r.type = type;
        r.ratio = ratio;
        return r;
    }

    /** 内置法定节假日区间（近似，需对照官方校核）。仅作为初始/年度自动播种来源。 */
    private List<LocalDate> spansOf(int year) {
        List<LocalDate> all = new ArrayList<>();
        // 2026 已公布安排（近似）
        if (year == 2026) {
            addSpan(all, 2026, 1, 1, 1, 1);
            addSpan(all, 2026, 2, 17, 2, 23);
            addSpan(all, 2026, 4, 4, 4, 6);
            addSpan(all, 2026, 5, 1, 5, 5);
            addSpan(all, 2026, 6, 19, 6, 21);
            addSpan(all, 2026, 9, 25, 9, 27);
            addSpan(all, 2026, 10, 1, 10, 7);
        }
        // 2027 近似（待对照官方校核）
        if (year == 2027) {
            addSpan(all, 2027, 1, 1, 1, 1);
            addSpan(all, 2027, 2, 6, 2, 12);
            addSpan(all, 2027, 4, 5, 4, 7);
            addSpan(all, 2027, 5, 1, 5, 3);
            addSpan(all, 2027, 6, 9, 6, 11);
            addSpan(all, 2027, 9, 15, 9, 17);
            addSpan(all, 2027, 10, 1, 10, 7);
        }
        return all;
    }

    private void addSpan(List<LocalDate> all, int y, int m1, int d1, int m2, int d2) {
        LocalDate s = LocalDate.of(y, m1, d1);
        LocalDate e = LocalDate.of(y, m2, d2);
        while (!s.isAfter(e)) {
            all.add(s);
            s = s.plusDays(1);
        }
    }
}
