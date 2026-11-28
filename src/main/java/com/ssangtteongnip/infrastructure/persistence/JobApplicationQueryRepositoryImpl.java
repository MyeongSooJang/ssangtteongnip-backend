package com.ssangtteongnip.infrastructure.persistence;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ssangtteongnip.domain.jobapplication.JobApplication;
import com.ssangtteongnip.domain.jobapplication.JobApplicationStatus;
import jakarta.persistence.EntityManager;

import java.util.List;

import static com.ssangtteongnip.domain.jobapplication.QJobApplication.jobApplication;

// Spring Data가 JobApplicationQueryRepository의 구현으로 인식하는 이름이다 (인터페이스 이름 + Impl).
public class JobApplicationQueryRepositoryImpl implements JobApplicationQueryRepository {

    private final JPAQueryFactory queryFactory;

    public JobApplicationQueryRepositoryImpl(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public List<JobApplication> findFilteredByStatus(Long userId, JobApplicationStatus status) {
        return queryFactory.selectFrom(jobApplication)
                .where(jobApplication.userId.eq(userId), statusEquals(status))
                .fetch();
    }

    // null을 돌려주면 QueryDSL이 그 조건을 where에서 뺀다.
    private BooleanExpression statusEquals(JobApplicationStatus status) {
        return status == null ? null : jobApplication.status.eq(status);
    }
}
