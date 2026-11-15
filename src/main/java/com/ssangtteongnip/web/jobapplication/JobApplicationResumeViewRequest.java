package com.ssangtteongnip.web.jobapplication;

import com.ssangtteongnip.domain.jobapplication.ResumeView;
import jakarta.validation.constraints.NotNull;

public record JobApplicationResumeViewRequest(@NotNull ResumeView resumeView) {}
