package com.lynn.overtime.repository;

import com.lynn.overtime.entity.OvertimeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface OvertimeRecordRepository extends JpaRepository<OvertimeRecord, Long> {
    List<OvertimeRecord> findByMemberId(Long memberId);
    List<OvertimeRecord> findByMonth(String month);
    @Query("select distinct o.month from OvertimeRecord o order by o.month desc")
    List<String> findDistinctMonths();
    List<OvertimeRecord> findByMemberIdAndMonth(Long memberId, String month);

    @Query("select sum(o.validHours) from OvertimeRecord o where o.memberId = ?1")
    BigDecimal sumValidHoursByMember(Long memberId);

    @Query("select sum(o.compHours) from OvertimeRecord o where o.memberId = ?1")
    BigDecimal sumCompHoursByMember(Long memberId);

    @Query("select sum(o.validHours) from OvertimeRecord o where o.memberId = ?1 and o.month = ?2")
    BigDecimal sumValidHoursByMemberAndMonth(Long memberId, String month);

    @Query("select sum(o.compHours) from OvertimeRecord o where o.memberId = ?1 and o.month = ?2")
    BigDecimal sumCompHoursByMemberAndMonth(Long memberId, String month);

    List<OvertimeRecord> findByOvertimeDateBetween(LocalDate start, LocalDate end);
}
