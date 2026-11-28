package com.ssangtteongnip.domain.jobapplication;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository {
    JobApplication save(JobApplication application);
    Optional<JobApplication> findById(Long id);

    // 사용자의 지원 건 중 status인 것을 조회한다. status가 null이면 상태를 가리지 않고 모두 조회한다. 정렬은 보장하지 않는다.
    List<JobApplication> findFilteredByStatus(Long userId, JobApplicationStatus status);

    // 사용자의 지원 건을 구직 사이트별로 집계한다. 지원이 없는 사이트는 포함하지 않고,
    // 지원 건 수 내림차순, 같으면 사이트 이름 오름차순으로 정렬한다.
    List<JobSiteStatistics> summarizeByJobSite(Long userId);

    // 서류 제출 상태이면서 공고 마감일이 있는 모든 사용자의 지원 건을 조회한다. 정렬은 보장하지 않는다.
    // 마감일 + 1달이 지났는지는 조회하지 않고 도메인(JobApplication)이 판단한다.
    List<JobApplication> findPendingWithDeadline();
}
