package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.schedule.Schedule;
import com.ssangtteongnip.domain.schedule.ScheduleType;

import java.time.LocalDateTime;

public record ScheduleResponse(
        Long id,
        JobApplicationResponse application,
        String title,
        LocalDateTime scheduledAt,
        ScheduleType type,
        String address,
        String note,
        LocalDateTime createdAt) {

    public static ScheduleResponse convert(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                JobApplicationResponse.convert(schedule.getApplication()),
                schedule.getTitle(),
                schedule.getScheduledAt(),
                schedule.getType(),
                schedule.getAddress(),
                schedule.getNote(),
                schedule.getCreatedAt());
    }
}
