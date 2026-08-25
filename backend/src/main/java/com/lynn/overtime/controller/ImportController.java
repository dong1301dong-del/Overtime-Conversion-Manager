package com.lynn.overtime.controller;

import com.lynn.overtime.common.ApiResult;
import com.lynn.overtime.common.SecurityUtil;
import com.lynn.overtime.entity.SysUser;
import com.lynn.overtime.service.ImportService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/import")
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/members")
    public ApiResult<Object> members(@RequestParam("file") MultipartFile file) {
        SecurityUtil.require(SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK);
        return wrap(importService.importMembers(file));
    }

    @PostMapping("/overtime")
    public ApiResult<Object> overtime(@RequestParam("file") MultipartFile file) {
        SecurityUtil.require(SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK);
        return wrap(importService.importOvertime(file));
    }

    @PostMapping("/comp-usage")
    public ApiResult<Object> compUsage(@RequestParam("file") MultipartFile file) {
        SecurityUtil.require(SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK);
        return wrap(importService.importCompUsage(file));
    }

    private ApiResult<Object> wrap(ImportService.ImportResult r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", r.success);
        m.put("count", r.count);
        m.put("errors", r.errors);
        ApiResult<Object> res = new ApiResult<>();
        res.setCode(r.success ? 0 : 1);
        res.setMessage(r.success ? "导入成功" : "导入校验未通过，请修正后重试");
        res.setData(m);
        return res;
    }
}
