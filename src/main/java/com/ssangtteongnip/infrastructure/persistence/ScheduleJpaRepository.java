package com.ssangtteongnip.infrastructure.persistence;

import com.ssangtteongnip.domain.schedule.Schedule;
import com.ssangtteongnip.domain.schedule.ScheduleRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleJpaRepository extends JpaRepository<Schedule, Long>, ScheduleRepository {
    // findInPeriod는 ScheduleJpaRepositoryImpl(QueryDSL)이 구현한다.
}
