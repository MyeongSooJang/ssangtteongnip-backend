package com.ssangtteongnip.web.jobapplication;

import com.ssangtteongnip.application.service.command.RegisterJobApplicationCommand;
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
    public RegisterJobApplicationCommand toCommand() {
        return new RegisterJobApplicationCommand(companyId, position, appliedDate, jobSite, deadline, resumeView);
    }
}
