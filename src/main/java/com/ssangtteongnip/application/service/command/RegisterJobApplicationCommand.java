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
) {}
