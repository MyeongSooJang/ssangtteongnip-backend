package com.ssangtteongnip.application;

import com.ssangtteongnip.application.service.CompanyService;
import com.ssangtteongnip.application.service.JobApplicationService;
import com.ssangtteongnip.application.service.dto.JobApplicationResponse;
import com.ssangtteongnip.application.service.dto.JobSiteStatisticsResponse;
import com.ssangtteongnip.application.service.request.JobApplicationRequest;
import com.ssangtteongnip.domain.company.Company;
import com.ssangtteongnip.domain.jobapplication.JobApplication;
import com.ssangtteongnip.domain.jobapplication.JobApplicationRepository;
import com.ssangtteongnip.domain.jobapplication.JobApplicationStatus;
import com.ssangtteongnip.domain.jobapplication.JobSite;
import com.ssangtteongnip.domain.jobapplication.JobSiteStatistics;
import com.ssangtteongnip.domain.jobapplication.ResumeView;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long APPLICATION_ID = 10L;

    @Mock JobApplicationRepository jobApplicationRepository;
    @Mock CompanyService companyService;
    @InjectMocks JobApplicationService jobApplicationService;

    private JobApplication application() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        return JobApplication.create(OWNER_ID, company, "백엔드 개발자", LocalDate.of(2026, 11, 1));
    }

    private JobApplication applicationWithDeadline(LocalDate deadline) {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        return JobApplication.create(OWNER_ID, company, "백엔드 개발자", LocalDate.of(2026, 10, 1),
                JobSite.SARAMIN, deadline, ResumeView.NOT_VIEWED);
    }

    @Test
    void 인증된_사용자의_지원_건으로_등록된다() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        given(companyService.findById(1L)).willReturn(company);
        given(jobApplicationRepository.save(any(JobApplication.class))).willAnswer(i -> i.getArgument(0));

        JobApplicationResponse response = jobApplicationService.register(7L,
                new JobApplicationRequest(1L, "백엔드 개발자", LocalDate.of(2026, 11, 1), JobSite.SARAMIN, null, null));

        ArgumentCaptor<JobApplication> saved = ArgumentCaptor.forClass(JobApplication.class);
        verify(jobApplicationRepository).save(saved.capture());
        assertThat(saved.getValue().isOwnedBy(7L)).isTrue();
        assertThat(saved.getValue().getCompany()).isSameAs(company);
        assertThat(response.status()).isEqualTo(JobApplicationStatus.RESUME_SUBMITTED);
        assertThat(response.jobSite()).isEqualTo(JobSite.SARAMIN);
    }

    @Test
    void 마감일과_열람_여부를_생략하면_마감일은_비고_미열람으로_등록된다() {
        given(companyService.findById(1L)).willReturn(Company.create("카카오", "IT", "https://kakao.com"));
        given(jobApplicationRepository.save(any(JobApplication.class))).willAnswer(i -> i.getArgument(0));

        JobApplicationResponse response = jobApplicationService.register(7L,
                new JobApplicationRequest(1L, "백엔드 개발자", LocalDate.of(2026, 11, 1), JobSite.SARAMIN, null, null));

        assertThat(response.deadline()).isNull();
        assertThat(response.resumeView()).isEqualTo(ResumeView.NOT_VIEWED);
    }

    @Test
    void 마감일과_열람_여부를_지정해_등록할_수_있다() {
        given(companyService.findById(1L)).willReturn(Company.create("카카오", "IT", "https://kakao.com"));
        given(jobApplicationRepository.save(any(JobApplication.class))).willAnswer(i -> i.getArgument(0));

        JobApplicationResponse response = jobApplicationService.register(7L,
                new JobApplicationRequest(1L, "백엔드 개발자", LocalDate.of(2026, 11, 1), JobSite.SARAMIN,
                        LocalDate.of(2026, 11, 30), ResumeView.VIEWED));

        assertThat(response.deadline()).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(response.resumeView()).isEqualTo(ResumeView.VIEWED);
    }

    @Test
    void 소유자는_자신의_지원_건의_마감일을_바꿀_수_있다() {
        JobApplication app = application();
        given(jobApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(app));

        jobApplicationService.changeDeadline(OWNER_ID, APPLICATION_ID,
                LocalDate.of(2026, 11, 30));

        assertThat(app.getDeadline()).isEqualTo(LocalDate.of(2026, 11, 30));
    }

    @Test
    void 다른_사용자의_지원_건은_마감일을_바꿀_수_없다() {
        JobApplication app = application();
        given(jobApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(app));

        assertErrorCode(
                () -> jobApplicationService.changeDeadline(OTHER_USER_ID, APPLICATION_ID,
                        LocalDate.of(2026, 11, 30)),
                ErrorCode.APPLICATION_NOT_FOUND);
        assertThat(app.getDeadline()).isNull();
    }

    @Test
    void 소유자는_자신의_지원_건의_열람_여부를_바꿀_수_있다() {
        JobApplication app = application();
        given(jobApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(app));

        jobApplicationService.changeResumeView(OWNER_ID, APPLICATION_ID, ResumeView.VIEWED);

        assertThat(app.getResumeView()).isEqualTo(ResumeView.VIEWED);
    }

    @Test
    void 다른_사용자의_지원_건은_열람_여부를_바꿀_수_없다() {
        JobApplication app = application();
        given(jobApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(app));

        assertErrorCode(
                () -> jobApplicationService.changeResumeView(OTHER_USER_ID, APPLICATION_ID,
                        ResumeView.VIEWED),
                ErrorCode.APPLICATION_NOT_FOUND);
        assertThat(app.getResumeView()).isEqualTo(ResumeView.NOT_VIEWED);
    }

    @Test
    void 마감일_한_달이_지난_지원만_불합격으로_바꾸고_바꾼_건_수를_돌려준다() {
        JobApplication expired = applicationWithDeadline(LocalDate.of(2026, 10, 10));
        JobApplication waiting = applicationWithDeadline(LocalDate.of(2026, 10, 20));
        given(jobApplicationRepository.findPendingWithDeadline()).willReturn(List.of(expired, waiting));

        int rejected = jobApplicationService.rejectExpiredApplications(LocalDateTime.of(2026, 11, 11, 7, 0));

        assertThat(rejected).isEqualTo(1);
        assertThat(expired.getStatus()).isEqualTo(JobApplicationStatus.REJECTED);
        assertThat(expired.getAutoRejectedAt()).isEqualTo(LocalDateTime.of(2026, 11, 11, 7, 0));
        assertThat(waiting.getStatus()).isEqualTo(JobApplicationStatus.RESUME_SUBMITTED);
    }

    @Test
    void 자동_불합격_대상이_없으면_0건을_돌려준다() {
        given(jobApplicationRepository.findPendingWithDeadline()).willReturn(List.of());

        int rejected = jobApplicationService.rejectExpiredApplications(LocalDateTime.of(2026, 11, 11, 7, 0));

        assertThat(rejected).isZero();
    }

    @Test
    void 소유자는_자신의_지원_건을_조회할_수_있다() {
        JobApplication app = application();
        given(jobApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(app));

        JobApplication found = jobApplicationService.findById(OWNER_ID, APPLICATION_ID);

        assertThat(found).isSameAs(app);
    }

    @Test
    void 소유자는_자신의_지원_건_상세를_회사와_함께_조회할_수_있다() {
        given(jobApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application()));

        JobApplicationResponse response = jobApplicationService.findDetail(OWNER_ID, APPLICATION_ID);

        assertThat(response.position()).isEqualTo("백엔드 개발자");
        assertThat(response.company().name()).isEqualTo("카카오");
    }

    @Test
    void 다른_사용자의_지원_건은_상세를_조회할_수_없다() {
        given(jobApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application()));

        assertErrorCode(() -> jobApplicationService.findDetail(OTHER_USER_ID, APPLICATION_ID),
                ErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    void 소유자는_자신의_지원_건의_구직_사이트를_바꿀_수_있다() {
        JobApplication app = application();
        given(jobApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(app));

        jobApplicationService.changeJobSite(OWNER_ID, APPLICATION_ID, JobSite.GROUPBY);

        assertThat(app.getJobSite()).isEqualTo(JobSite.GROUPBY);
    }

    @Test
    void 다른_사용자의_지원_건은_구직_사이트를_바꿀_수_없다() {
        JobApplication app = application();
        app.changeJobSite(JobSite.WANTED);
        given(jobApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(app));

        assertErrorCode(
                () -> jobApplicationService.changeJobSite(OTHER_USER_ID, APPLICATION_ID, JobSite.OTHER),
                ErrorCode.APPLICATION_NOT_FOUND);
        assertThat(app.getJobSite()).isEqualTo(JobSite.WANTED);
    }

    @Test
    void 존재하지_않는_지원_건은_조회할_수_없다() {
        given(jobApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> jobApplicationService.findById(OWNER_ID, APPLICATION_ID), ErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    void 다른_사용자의_지원_건은_조회할_수_없다() {
        given(jobApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application()));

        assertErrorCode(() -> jobApplicationService.findById(OTHER_USER_ID, APPLICATION_ID), ErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    void 사이트별_집계는_서류_통과율을_계산해_응답한다() {
        given(jobApplicationRepository.summarizeByJobSite(OWNER_ID)).willReturn(List.of(
                new JobSiteStatistics(JobSite.SARAMIN, 10, 3),
                new JobSiteStatistics(JobSite.WANTED, 2, 0)));

        List<JobSiteStatisticsResponse> responses = jobApplicationService.summarizeByJobSite(OWNER_ID);

        assertThat(responses).containsExactly(
                new JobSiteStatisticsResponse(JobSite.SARAMIN, 10, 3, 0.3),
                new JobSiteStatisticsResponse(JobSite.WANTED, 2, 0, 0.0));
    }

    @Test
    void 불합격은_직접_전환할_수_있다() {
        JobApplication app = application();
        given(jobApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(app));

        jobApplicationService.decideResult(OWNER_ID, APPLICATION_ID, JobApplicationStatus.REJECTED);

        assertThat(app.getStatus()).isEqualTo(JobApplicationStatus.REJECTED);
    }

    @Test
    void 면접으로는_직접_전환할_수_없고_면접_일정을_등록해야_한다() {
        JobApplication app = application();
        given(jobApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(app));

        assertErrorCode(
                () -> jobApplicationService.decideResult(OWNER_ID, APPLICATION_ID, JobApplicationStatus.INTERVIEW),
                ErrorCode.INTERVIEW_SCHEDULE_REQUIRED);
        assertThat(app.getStatus()).isEqualTo(JobApplicationStatus.RESUME_SUBMITTED);
    }

    @Test
    void 다른_사용자의_지원_건은_상태를_바꿀_수_없다() {
        JobApplication app = application();
        given(jobApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(app));

        assertErrorCode(
                () -> jobApplicationService.decideResult(OTHER_USER_ID, APPLICATION_ID, JobApplicationStatus.REJECTED),
                ErrorCode.APPLICATION_NOT_FOUND);
        assertThat(app.getStatus()).isEqualTo(JobApplicationStatus.RESUME_SUBMITTED);
    }
}
