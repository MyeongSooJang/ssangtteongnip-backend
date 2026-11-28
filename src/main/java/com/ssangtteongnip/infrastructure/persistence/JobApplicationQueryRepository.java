package com.ssangtteongnip.infrastructure.persistence;

import com.ssangtteongnip.domain.jobapplication.JobApplication;
import com.ssangtteongnip.domain.jobapplication.JobApplicationStatus;

import java.util.List;

// QueryDSL로 구현하는 조회. JobApplicationJpaRepository가 상속하고 구현체는 JobApplicationQueryRepositoryImpl이다.
interface JobApplicationQueryRepository {
    List<JobApplication> findFilteredByStatus(Long userId, JobApplicationStatus status);
}
