package com.ssangtteongnip.infrastructure;

import com.ssangtteongnip.domain.jobapplication.JobApplication;
import com.ssangtteongnip.domain.jobapplication.JobApplicationRepository;
import com.ssangtteongnip.domain.jobapplication.JobSite;
import com.ssangtteongnip.domain.jobapplication.ResumeView;
import com.ssangtteongnip.domain.company.Company;
import com.ssangtteongnip.domain.company.CompanyRepository;
import com.ssangtteongnip.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class JobApplicationRepositoryTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final LocalDate DEADLINE = LocalDate.of(2026, 10, 10);

    @Autowired CompanyRepository companyRepository;
    @Autowired JobApplicationRepository jobApplicationRepository;
    @Autowired EntityManager em;

    private Company company;

    @BeforeEach
    void setUp() {
        company = companyRepository.save(Company.create("카카오", "IT", "https://kakao.com"));
    }

    private JobApplication save(String position, LocalDate deadline) {
        return save(USER_ID, position, deadline, ResumeView.NOT_VIEWED);
    }

    private JobApplication save(Long userId, String position, LocalDate deadline, ResumeView resumeView) {
        return jobApplicationRepository.save(JobApplication.create(userId, company, position,
                LocalDate.of(2026, 10, 1), JobSite.SARAMIN, deadline, resumeView));
    }

    @Test
    void 서류_제출_상태이고_마감일이_있는_지원만_조회한다() {
        save("대기 중", DEADLINE);
        save(OTHER_USER_ID, "다른 사용자의 대기 중", DEADLINE, ResumeView.NOT_VIEWED);
        save("마감일 없음", null);
        JobApplication interview = save("면접 중", DEADLINE);
        interview.startInterview(LocalDateTime.of(2026, 10, 20, 10, 0));
        JobApplication rejected = save("불합격", DEADLINE);
        rejected.reject();
        em.flush();
        em.clear();

        List<JobApplication> found = jobApplicationRepository.findPendingWithDeadline();

        assertThat(found).extracting(JobApplication::getPosition).containsExactlyInAnyOrder("대기 중", "다른 사용자의 대기 중");
    }

    @Test
    void 열람_여부는_저장한_값으로_조회된다() {
        JobApplication saved = save(USER_ID, "백엔드 개발자", DEADLINE, ResumeView.VIEWED);
        em.flush();
        em.clear();

        JobApplication found = jobApplicationRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getResumeView()).isEqualTo(ResumeView.VIEWED);
        assertThat(found.getDeadline()).isEqualTo(DEADLINE);
    }
}
