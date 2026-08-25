package com.lynn.overtime.controller;

import com.lynn.overtime.common.ApiResult;
import com.lynn.overtime.common.SecurityUtil;
import com.lynn.overtime.entity.SysMessage;
import com.lynn.overtime.entity.SysUser;
import com.lynn.overtime.service.MessageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public ApiResult<List<SysMessage>> list() {
        return ApiResult.ok(messageService.visibleForCurrentUser());
    }

    @PostMapping("/{id}/read")
    public ApiResult<Void> read(@PathVariable Long id) {
        messageService.markRead(id);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        SecurityUtil.require(SysUser.ROLE_ADMIN, SysUser.ROLE_CLERK);
        messageService.delete(id);
        return ApiResult.ok();
    }
}
