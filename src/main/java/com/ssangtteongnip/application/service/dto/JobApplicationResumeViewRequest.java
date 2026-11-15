package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.jobapplication.ResumeView;
import jakarta.validation.constraints.NotNull;

public record JobApplicationResumeViewRequest(@NotNull ResumeView resumeView) {}
