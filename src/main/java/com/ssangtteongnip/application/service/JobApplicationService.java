package com.ssangtteongnip.application.service;

import com.ssangtteongnip.application.service.dto.JobApplicationResponse;
import com.ssangtteongnip.application.service.dto.JobSiteStatisticsResponse;
import com.ssangtteongnip.application.service.request.JobApplicationRequest;
import com.ssangtteongnip.domain.company.Company;
import com.ssangtteongnip.domain.jobapplication.JobApplication;
import com.ssangtteongnip.domain.jobapplication.JobApplicationRepository;
import com.ssangtteongnip.domain.jobapplication.JobApplicationStatus;
import com.ssangtteongnip.domain.jobapplication.JobSite;
import com.ssangtteongnip.domain.jobapplication.ResumeView;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final CompanyService companyService;

    @Transactional
    public JobApplicationResponse register(Long userId, JobApplicationRequest request) {
        Company company = companyService.findById(request.companyId());
        JobApplication saved = jobApplicationRepository.save(
                JobApplication.create(userId, company, request.position(), request.appliedDate(),
                        request.jobSite(), request.deadline(), request.resumeView())
        );
        return JobApplicationResponse.convert(saved);
    }

    // status가 null이면 상태를 가리지 않고 모두 조회한다.
    public List<JobApplicationResponse> findAll(Long userId, JobApplicationStatus status) {
        return jobApplicationRepository.findFilteredByStatus(userId, status).stream()
                .map(JobApplicationResponse::convert)
                .toList();
    }

    public List<JobSiteStatisticsResponse> summarizeByJobSite(Long userId) {
        return jobApplicationRepository.summarizeByJobSite(userId).stream()
                .map(JobSiteStatisticsResponse::convert)
                .toList();
    }

    public JobApplication findById(Long userId, Long id) {
        JobApplication application = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
        application.validateOwner(userId);
        return application;
    }

    public JobApplicationResponse findDetail(Long userId, Long id) {
        return JobApplicationResponse.convert(findById(userId, id));
    }

    @Transactional
    public JobApplicationResponse changeJobSite(Long userId, Long id, JobSite jobSite) {
        JobApplication application = findById(userId, id);
        application.changeJobSite(jobSite);
        return JobApplicationResponse.convert(application);
    }

    @Transactional
    public JobApplicationResponse changeDeadline(Long userId, Long id, LocalDate deadline) {
        JobApplication application = findById(userId, id);
        application.changeDeadline(deadline);
        return JobApplicationResponse.convert(application);
    }

    @Transactional
    public JobApplicationResponse changeResumeView(Long userId, Long id, ResumeView resumeView) {
        JobApplication application = findById(userId, id);
        application.changeResumeView(resumeView);
        return JobApplicationResponse.convert(application);
    }

    @Transactional
    public JobApplicationResponse decideResult(Long userId, Long id, JobApplicationStatus status) {
        JobApplication application = findById(userId, id);
        status.apply(application);
        return JobApplicationResponse.convert(application);
    }

    // 마감일 + 1달이 지난 서류 제출 건을 불합격으로 바꾸고, 바꾼 건 수를 돌려준다.
    @Transactional
    public int rejectExpiredApplications(LocalDateTime now) {
        int rejected = 0;
        for (JobApplication application : jobApplicationRepository.findPendingWithDeadline()) {
            if (application.rejectIfDeadlineExpired(now)) {
                rejected++;
            }
        }
        return rejected;
    }
}
