package com.lynn.overtime.repository;

import com.lynn.overtime.entity.SysMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SysMessageRepository extends JpaRepository<SysMessage, Long> {
    // 按角色 + 指定人接收（或角色为空表示该消息面向所有人）
    @Query("select m from SysMessage m where " +
           "(m.receiverRole is null or m.receiverRole = ?1 or m.receiverRole = 'ALL') " +
           "and (m.receiverUser is null or m.receiverUser = ?2) " +
           "order by m.createdAt desc")
    List<SysMessage> findVisible(String role, Long userId);

    List<SysMessage> findByReceiverUserAndIsRead(Long userId, Integer isRead);
    List<SysMessage> findByTypeAndLevel(String type, Integer level);
}
