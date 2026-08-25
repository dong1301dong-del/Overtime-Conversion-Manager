package com.lynn.overtime.repository;

import com.lynn.overtime.entity.CompUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CompUsageRepository extends JpaRepository<CompUsage, Long> {
    List<CompUsage> findByMemberId(Long memberId);
    List<CompUsage> findByMonth(String month);
    List<CompUsage> findByMemberIdAndMonth(Long memberId, String month);

    @Query("select sum(c.hours) from CompUsage c where c.memberId = ?1")
    BigDecimal sumHoursByMember(Long memberId);

    @Query("select sum(c.hours) from CompUsage c where c.memberId = ?1 and c.month = ?2")
    BigDecimal sumHoursByMemberAndMonth(Long memberId, String month);
}
