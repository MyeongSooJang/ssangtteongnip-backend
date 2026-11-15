package com.ssangtteongnip.application.service.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record JobApplicationDeadlineRequest(@NotNull LocalDate deadline) {}
