package com.lynn.overtime.repository;

import com.lynn.overtime.entity.HolidayCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayCalendarRepository extends JpaRepository<HolidayCalendar, Long> {
    Optional<HolidayCalendar> findByHolidayDate(LocalDate date);
    List<HolidayCalendar> findByHolidayDateBetween(LocalDate start, LocalDate end);
    long countByHolidayDateBetween(LocalDate start, LocalDate end);
    List<HolidayCalendar> findByKindAndAuto(String kind, Integer auto);
    void deleteByHolidayDate(LocalDate date);
}
