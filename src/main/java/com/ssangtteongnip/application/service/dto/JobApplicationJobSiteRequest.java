package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.jobapplication.JobSite;
import jakarta.validation.constraints.NotNull;

public record JobApplicationJobSiteRequest(@NotNull JobSite jobSite) {}
