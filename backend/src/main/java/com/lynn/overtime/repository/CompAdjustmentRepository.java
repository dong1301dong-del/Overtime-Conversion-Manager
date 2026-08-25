package com.lynn.overtime.repository;

import com.lynn.overtime.entity.CompAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CompAdjustmentRepository extends JpaRepository<CompAdjustment, Long> {
    List<CompAdjustment> findByMemberId(Long memberId);
    List<CompAdjustment> findByMonth(String month);
    List<CompAdjustment> findByMemberIdAndMonth(Long memberId, String month);

    @Query("select sum(a.hours) from CompAdjustment a where a.memberId = ?1")
    BigDecimal sumHoursByMember(Long memberId);

    @Query("select sum(a.hours) from CompAdjustment a where a.memberId = ?1 and a.month = ?2")
    BigDecimal sumHoursByMemberAndMonth(Long memberId, String month);
}
