package com.ssangtteongnip.application.service.command;

import com.ssangtteongnip.domain.jobapplication.JobSite;
import com.ssangtteongnip.domain.jobapplication.ResumeView;

import java.time.LocalDate;

public record RegisterJobApplicationCommand(
        Long companyId,
        String position,
        LocalDate appliedDate,
        JobSite jobSite,
        LocalDate deadline,
        ResumeView resumeView
) {
    // 열람 여부를 생략하면 미열람으로 등록한다.
    public RegisterJobApplicationCommand {
        resumeView = resumeView == null ? ResumeView.NOT_VIEWED : resumeView;
    }
}
