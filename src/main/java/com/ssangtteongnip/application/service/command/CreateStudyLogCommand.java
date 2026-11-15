package com.ssangtteongnip.application.service.command;

import java.time.LocalDate;

public record CreateStudyLogCommand(
        LocalDate studyDate,
        String title,
        String content,
        int studyMinutes
) {}
