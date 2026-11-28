package com.ssangtteongnip.infrastructure.persistence;

import com.ssangtteongnip.domain.schedule.Schedule;

import java.time.LocalDateTime;
import java.util.List;

// QueryDSL로 구현하는 조회. ScheduleJpaRepository가 상속하고 구현체는 ScheduleQueryRepositoryImpl이다.
interface ScheduleQueryRepository {
    List<Schedule> findInPeriod(Long userId, LocalDateTime from, LocalDateTime to);
}
