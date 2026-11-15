package com.ssangtteongnip.application.service.command;

import com.ssangtteongnip.domain.schedule.ScheduleType;

import java.time.LocalDateTime;

public record RegisterScheduleCommand(
        Long applicationId,
        String title,
        LocalDateTime scheduledAt,
        ScheduleType type,
        String address,
        String note
) {}
