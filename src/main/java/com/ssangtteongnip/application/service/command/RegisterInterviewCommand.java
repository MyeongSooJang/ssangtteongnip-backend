package com.ssangtteongnip.application.service.command;

import java.time.LocalDateTime;

public record RegisterInterviewCommand(
        int round,
        LocalDateTime interviewDate,
        String location,
        String note,
        Integer score
) {}
