package com.lynn.overtime.service;

import com.lynn.overtime.common.CurrentUserHolder;
import com.lynn.overtime.entity.AuditLog;
import com.lynn.overtime.entity.SysMessage;
import com.lynn.overtime.repository.AuditLogRepository;
import com.lynn.overtime.repository.SysMessageRepository;
import com.lynn.overtime.repository.SysUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MessageService {

    private final SysMessageRepository msgRepo;
    private final SysUserRepository userRepo;
    private final AuditLogRepository auditRepo;

    public MessageService(SysMessageRepository msgRepo, SysUserRepository userRepo, AuditLogRepository auditRepo) {
        this.msgRepo = msgRepo;
        this.userRepo = userRepo;
        this.auditRepo = auditRepo;
    }

    /** 当前用户可见消息：按角色 + 指定接收人过滤 */
    public List<SysMessage> visibleForCurrentUser() {
        com.lynn.overtime.common.AuthInfo info = CurrentUserHolder.get();
        if (info == null) return List.of();
        return msgRepo.findVisible(info.getRole(), info.getUserId());
    }

    @Transactional
    public void markRead(Long id) {
        SysMessage m = msgRepo.findById(id).orElse(null);
        if (m != null) {
            m.setIsRead(1);
            msgRepo.save(m);
        }
    }

    @Transactional
    public void delete(Long id) {
        msgRepo.deleteById(id);
    }

    /** 发送消息（系统内部调用） */
    @Transactional
    public void send(String type, String content, String receiverRole, Long receiverUser, int level) {
        SysMessage m = new SysMessage();
        m.setType(type);
        m.setContent(content);
        m.setReceiverRole(receiverRole);
        m.setReceiverUser(receiverUser);
        m.setLevel(level);
        m.setIsRead(0);
        msgRepo.save(m);
    }

    public void sendToRoles(String type, String content, int level, String... roles) {
        for (String r : roles) send(type, content, r, null, level);
    }

    private void audit(String action, String detail) {
        com.lynn.overtime.common.AuthInfoHolder.log(auditRepo, action, detail);
    }
}
