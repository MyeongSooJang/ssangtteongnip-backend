package com.ssangtteongnip.application.service;

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
    public JobApplication register(Long userId, JobApplicationRequest request) {
        Company company = companyService.findById(request.companyId());
        return jobApplicationRepository.save(
                JobApplication.create(userId, company, request.position(), request.appliedDate(),
                        request.jobSite(), request.deadline(), request.resumeView())
        );
    }

    public List<JobApplication> findAll(Long userId) {
        return jobApplicationRepository.findByUserId(userId);
    }

    public List<JobApplication> findByStatus(Long userId, JobApplicationStatus status) {
        return jobApplicationRepository.findByUserIdAndStatus(userId, status);
    }

    public List<JobSiteStatisticsResponse> summarizeByJobSite(Long userId) {
        return jobApplicationRepository.summarizeByJobSite(userId).stream()
                .map(JobSiteStatisticsResponse::from)
                .toList();
    }

    public JobApplication findById(Long userId, Long id) {
        JobApplication application = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
        application.validateOwner(userId);
        return application;
    }

    @Transactional
    public JobApplication changeJobSite(Long userId, Long id, JobSite jobSite) {
        JobApplication application = findById(userId, id);
        application.changeJobSite(jobSite);
        return application;
    }

    @Transactional
    public JobApplication changeDeadline(Long userId, Long id, LocalDate deadline) {
        JobApplication application = findById(userId, id);
        application.changeDeadline(deadline);
        return application;
    }

    @Transactional
    public JobApplication changeResumeView(Long userId, Long id, ResumeView resumeView) {
        JobApplication application = findById(userId, id);
        application.changeResumeView(resumeView);
        return application;
    }

    @Transactional
    public JobApplication decideResult(Long userId, Long id, JobApplicationStatus status) {
        JobApplication application = findById(userId, id);
        status.apply(application);
        return application;
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
