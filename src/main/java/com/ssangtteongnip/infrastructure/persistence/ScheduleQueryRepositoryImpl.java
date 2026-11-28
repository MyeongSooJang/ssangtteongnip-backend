package com.ssangtteongnip.infrastructure.persistence;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ssangtteongnip.domain.schedule.Schedule;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static com.ssangtteongnip.domain.schedule.QSchedule.schedule;

// Spring Data가 ScheduleQueryRepository의 구현으로 인식하는 이름이다 (인터페이스 이름 + Impl).
public class ScheduleQueryRepositoryImpl implements ScheduleQueryRepository {

    private final JPAQueryFactory queryFactory;

    public ScheduleQueryRepositoryImpl(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public List<Schedule> findInPeriod(Long userId, LocalDateTime from, LocalDateTime to) {
        return queryFactory.selectFrom(schedule)
                .where(schedule.application.userId.eq(userId), scheduledAtFrom(from), scheduledAtUntil(to))
                .fetch();
    }

    // null을 돌려주면 QueryDSL이 그 조건을 where에서 뺀다.
    private BooleanExpression scheduledAtFrom(LocalDateTime from) {
        return from == null ? null : schedule.scheduledAt.goe(from);
    }

    private BooleanExpression scheduledAtUntil(LocalDateTime to) {
        return to == null ? null : schedule.scheduledAt.loe(to);
    }
}
