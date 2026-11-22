package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.jobapplication.JobApplication;
import com.ssangtteongnip.domain.jobapplication.JobApplicationStatus;
import com.ssangtteongnip.domain.jobapplication.JobSite;
import com.ssangtteongnip.domain.jobapplication.ResumeView;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 소유자 식별값(userId)은 담지 않는다.
public record JobApplicationResponse(
        Long id,
        AppliedCompanyResponse company,
        String position,
        JobApplicationStatus status,
        LocalDate appliedDate,
        JobSite jobSite,
        LocalDate deadline,
        ResumeView resumeView,
        LocalDateTime autoRejectedAt,
        boolean documentPassed,
        LocalDateTime documentPassedAt,
        LocalDateTime createdAt) {

    public static JobApplicationResponse convert(JobApplication application) {
        return new JobApplicationResponse(
                application.getId(),
                AppliedCompanyResponse.convert(application.getCompany()),
                application.getPosition(),
                application.getStatus(),
                application.getAppliedDate(),
                application.getJobSite(),
                application.getDeadline(),
                application.getResumeView(),
                application.getAutoRejectedAt(),
                application.isDocumentPassed(),
                application.getDocumentPassedAt(),
                application.getCreatedAt());
    }
}
