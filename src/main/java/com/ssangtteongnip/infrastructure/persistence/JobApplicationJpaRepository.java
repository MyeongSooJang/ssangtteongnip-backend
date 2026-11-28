package com.ssangtteongnip.infrastructure.persistence;

import com.ssangtteongnip.domain.jobapplication.JobApplication;
import com.ssangtteongnip.domain.jobapplication.JobApplicationRepository;
import com.ssangtteongnip.domain.jobapplication.JobSiteStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobApplicationJpaRepository extends JpaRepository<JobApplication, Long>, JobApplicationRepository, JobApplicationQueryRepository {
    @Override
    @Query("""
            select a.jobSite, count(a), count(a.documentPassedAt)
            from JobApplication a
            where a.userId = :userId
            group by a.jobSite
            order by count(a) desc, a.jobSite asc
            """)
    List<JobSiteStatistics> summarizeByJobSite(@Param("userId") Long userId);

    @Override
    @Query("""
            select a from JobApplication a
            where a.status = RESUME_SUBMITTED
              and a.deadline is not null
            """)
    List<JobApplication> findPendingWithDeadline();
}
