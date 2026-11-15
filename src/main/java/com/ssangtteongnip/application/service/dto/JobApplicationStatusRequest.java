package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.jobapplication.JobApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record JobApplicationStatusRequest(@NotNull JobApplicationStatus status) {}
