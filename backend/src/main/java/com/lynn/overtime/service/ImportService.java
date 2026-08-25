package com.lynn.overtime.service;

import com.lynn.overtime.common.BizException;
import com.lynn.overtime.common.DateUtil;
import com.lynn.overtime.common.NumberUtil;
import com.lynn.overtime.entity.Member;
import com.lynn.overtime.repository.MemberRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ImportService {

    public static class ImportError {
        public int row;
        public String reason;

        public ImportError(int row, String reason) {
            this.row = row;
            this.reason = reason;
        }
    }

    public static class ImportResult {
        public boolean success;
        public int count;
        public List<ImportError> errors = new ArrayList<>();

        public static ImportResult fail(List<ImportError> errors) {
            ImportResult r = new ImportResult();
            r.success = false;
            r.errors = errors;
            return r;
        }

        public static ImportResult ok(int count) {
            ImportResult r = new ImportResult();
            r.success = true;
            r.count = count;
            return r;
        }
    }

    private final MemberRepository memberRepo;
    private final MemberService memberService;
    private final OvertimeService overtimeService;
    private final CompUsageService compUsageService;

    public ImportService(MemberRepository memberRepo, MemberService memberService,
                         OvertimeService overtimeService, CompUsageService compUsageService) {
        this.memberRepo = memberRepo;
        this.memberService = memberService;
        this.overtimeService = overtimeService;
        this.compUsageService = compUsageService;
    }

    // ===================== 月度加班转调休-详表 =====================
    @Transactional
    public ImportResult importOvertime(MultipartFile file) {
        List<ImportError> errors = new ArrayList<>();
        List<RowData> rows = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheet("月度加班转调休-详表");
            if (sheet == null) sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // 跳过表头
                Row row = sheet.getRow(i);
                if (row == null) continue;
                int excelRow = i + 1;
                String name = cell(row, 1);
                String dateStr = cell(row, 2);
                String period = cell(row, 4);
                String type = cell(row, 5);
                String validHoursStr = cell(row, 7);
                if (name.isEmpty() && dateStr.isEmpty() && validHoursStr.isEmpty()) continue; // 空行
                RowData d = new RowData();
                d.excelRow = excelRow;
                d.name = name;
                d.dateStr = dateStr;
                d.period = period;
                d.type = type;
                d.validHoursStr = validHoursStr;
                d.punch = cell(row, 9);
                d.remark = cell(row, 10);
                rows.add(d);
            }
        } catch (Exception e) {
            return ImportResult.fail(List.of(new ImportError(0, "文件解析失败：" + e.getMessage())));
        }

        // 全量校验
        for (RowData d : rows) {
            if (d.name.isEmpty()) { errors.add(new ImportError(d.excelRow, "姓名不能为空")); continue; }
            Optional<Member> m = resolveMember(d.name);
            if (m.isEmpty()) { errors.add(new ImportError(d.excelRow, "姓名不存在：" + d.name)); continue; }
            if (d.dateStr.isEmpty()) { errors.add(new ImportError(d.excelRow, "加班日期不能为空")); continue; }
            LocalDate date;
            try { date = DateUtil.parse(d.dateStr); } catch (BizException be) { errors.add(new ImportError(d.excelRow, be.getMessage())); continue; }
            if (d.period.isEmpty()) { errors.add(new ImportError(d.excelRow, "有效时段不能为空")); continue; }
            if (!List.of("工作日", "周末", "法定节假日").contains(d.type)) { errors.add(new ImportError(d.excelRow, "类型须为 工作日/周末/法定节假日，实际：" + d.type)); continue; }
            BigDecimal vh;
            try { vh = NumberUtil.of(d.validHoursStr); } catch (Exception e) { errors.add(new ImportError(d.excelRow, "有效加班时长格式错误：" + d.validHoursStr)); continue; }
            if (vh.compareTo(BigDecimal.ZERO) < 0) { errors.add(new ImportError(d.excelRow, "有效加班时长须 ≥ 0")); continue; }
            d.member = m.get();
            d.date = date;
            d.validHours = vh;
        }

        if (!errors.isEmpty()) return ImportResult.fail(errors);

        // 全部通过 → 写入
        int count = 0;
        for (RowData d : rows) {
            overtimeService.saveManual(d.member.getId(), d.date, d.period, d.type, d.validHours, d.punch, d.remark);
            count++;
        }
        return ImportResult.ok(count);
    }

    // ===================== 调休使用记录 =====================
    @Transactional
    public ImportResult importCompUsage(MultipartFile file) {
        List<ImportError> errors = new ArrayList<>();
        List<CompRow> rows = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheet("调休使用记录");
            if (sheet == null) sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                int excelRow = i + 1;
                String name = cell(row, 1);
                String startStr = cell(row, 2);
                String endStr = cell(row, 3);
                String modeStr = cell(row, 4);
                String durationStr = cell(row, 5);
                if (name.isEmpty() && startStr.isEmpty() && durationStr.isEmpty()) continue;
                CompRow d = new CompRow();
                d.excelRow = excelRow; d.name = name; d.startStr = startStr; d.endStr = endStr;
                d.modeStr = modeStr; d.durationStr = durationStr; d.remark = cell(row, 6);
                rows.add(d);
            }
        } catch (Exception e) {
            return ImportResult.fail(List.of(new ImportError(0, "文件解析失败：" + e.getMessage())));
        }

        for (CompRow d : rows) {
            if (d.name.isEmpty()) { errors.add(new ImportError(d.excelRow, "姓名不能为空")); continue; }
            Optional<Member> m = resolveMember(d.name);
            if (m.isEmpty()) { errors.add(new ImportError(d.excelRow, "姓名不存在：" + d.name)); continue; }
            if (d.startStr.isEmpty()) { errors.add(new ImportError(d.excelRow, "使用开始日期不能为空")); continue; }
            LocalDate start;
            try { start = DateUtil.parse(d.startStr); } catch (BizException be) { errors.add(new ImportError(d.excelRow, be.getMessage())); continue; }
            LocalDate end = d.endStr.isEmpty() ? start : safeParse(d.endStr, d.excelRow, errors);
            if (end == null) continue;
            int mode;
            if ("小时".equals(d.modeStr) || "A".equalsIgnoreCase(d.modeStr)) mode = CompUsageService.MODE_HOUR;
            else if ("天数".equals(d.modeStr) || "B".equalsIgnoreCase(d.modeStr) || "C".equalsIgnoreCase(d.modeStr)) mode = CompUsageService.MODE_DAY;
            else { errors.add(new ImportError(d.excelRow, "模式须为 小时/天数，实际：" + d.modeStr)); continue; }
            BigDecimal dur;
            try { dur = NumberUtil.of(d.durationStr); } catch (Exception e) { errors.add(new ImportError(d.excelRow, "时长格式错误：" + d.durationStr)); continue; }
            if (mode == CompUsageService.MODE_DAY) {
                BigDecimal twice = dur.multiply(BigDecimal.valueOf(2));
                if (twice.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                    errors.add(new ImportError(d.excelRow, "天数须为 0.5 的整数倍（如 1.0/1.5/2.0/2.5…）")); continue;
                }
            }
            d.member = m.get(); d.start = start; d.end = end; d.mode = mode; d.duration = dur;
        }

        if (!errors.isEmpty()) return ImportResult.fail(errors);

        int count = 0;
        for (CompRow d : rows) {
            compUsageService.saveManual(d.member.getId(), d.start, d.end, d.mode, d.duration, d.remark);
            count++;
        }
        return ImportResult.ok(count);
    }

    // ===================== 成员清单 =====================
    @Transactional
    public ImportResult importMembers(MultipartFile file) {
        List<ImportError> errors = new ArrayList<>();
        List<MemberRow> rows = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheet("成员清单");
            if (sheet == null) sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                int excelRow = i + 1;
                String name = cell(row, 0);
                String username = cell(row, 1);
                String dept = cell(row, 2);
                String pwd = cell(row, 3);
                if (name.isEmpty() && username.isEmpty()) continue;
                MemberRow d = new MemberRow();
                d.excelRow = excelRow; d.name = name; d.username = username; d.dept = dept; d.pwd = pwd;
                rows.add(d);
            }
        } catch (Exception e) {
            return ImportResult.fail(List.of(new ImportError(0, "文件解析失败：" + e.getMessage())));
        }

        for (MemberRow d : rows) {
            if (d.name.isEmpty()) { errors.add(new ImportError(d.excelRow, "姓名不能为空")); continue; }
            if (!MemberService.isValidUsername(d.username)) { errors.add(new ImportError(d.excelRow, "用户名须为字母与数字组成，最长26位，不允许中文")); continue; }
            if (memberRepo.existsByUsername(d.username)) { errors.add(new ImportError(d.excelRow, "用户名已存在：" + d.username)); continue; }
            d.ready = true;
        }

        if (!errors.isEmpty()) return ImportResult.fail(errors);

        int count = 0;
        for (MemberRow d : rows) {
            memberService.create(d.name, d.username, d.dept, d.pwd.isEmpty() ? null : d.pwd);
            count++;
        }
        return ImportResult.ok(count);
    }

    private Optional<Member> resolveMember(String name) {
        List<Member> list = memberRepo.findByName(name);
        if (list.isEmpty()) return Optional.empty();
        return Optional.of(list.get(0));
    }

    private LocalDate safeParse(String s, int row, List<ImportError> errors) {
        try { return DateUtil.parse(s); } catch (BizException be) { errors.add(new ImportError(row, be.getMessage())); return null; }
    }

    private static String cell(Row row, int idx) {
        if (row == null) return "";
        Cell c = row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (c == null) return "";
        switch (c.getCellType()) {
            case STRING: return c.getStringCellValue().trim();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(c)) return c.getLocalDateTimeCellValue().toLocalDate().toString();
                double v = c.getNumericCellValue();
                if (v == Math.floor(v) && !String.valueOf(v).contains(".")) return String.valueOf((long) v);
                return String.valueOf(v);
            case BOOLEAN: return String.valueOf(c.getBooleanCellValue());
            case FORMULA: return c.getCellFormula();
            default: return "";
        }
    }

    // 内部数据载体
    private static class RowData {
        int excelRow; String name; String dateStr; String period; String type; String validHoursStr;
        String punch; String remark;
        Member member; LocalDate date; BigDecimal validHours;
    }
    private static class CompRow {
        int excelRow; String name; String startStr; String endStr; String modeStr; String durationStr; String remark;
        Member member; LocalDate start; LocalDate end; int mode; BigDecimal duration;
    }
    private static class MemberRow {
        int excelRow; String name; String username; String dept; String pwd; boolean ready;
    }
}
