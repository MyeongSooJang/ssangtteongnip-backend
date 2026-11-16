package com.ssangtteongnip.web.jobapplication;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record JobApplicationDeadlineRequest(@NotNull LocalDate deadline) {}
