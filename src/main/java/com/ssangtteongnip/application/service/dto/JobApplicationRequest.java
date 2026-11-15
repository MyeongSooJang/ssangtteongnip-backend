package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.jobapplication.JobSite;
import com.ssangtteongnip.domain.jobapplication.ResumeView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record JobApplicationRequest(
        @NotNull Long companyId,
        @NotBlank String position,
        @NotNull LocalDate appliedDate,
        @NotNull JobSite jobSite,
        LocalDate deadline,
        ResumeView resumeView
) {
    // 열람 여부를 생략하면 미열람으로 등록한다.
    public JobApplicationRequest {
        resumeView = resumeView == null ? ResumeView.NOT_VIEWED : resumeView;
    }
}
