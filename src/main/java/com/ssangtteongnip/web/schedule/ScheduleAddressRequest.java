package com.ssangtteongnip.web.schedule;

import jakarta.validation.constraints.NotBlank;

public record ScheduleAddressRequest(@NotBlank String address) {}
