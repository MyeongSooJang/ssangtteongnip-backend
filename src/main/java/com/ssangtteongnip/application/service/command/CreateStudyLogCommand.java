package com.ssangtteongnip.application.service.command;

import java.time.LocalDate;

public record CreateStudyLogCommand(
        LocalDate studyDate,
        String title,
        String content,
        Integer studyMinutes
) {
    // 공부 시간을 생략하면 0분으로 저장한다.
    public CreateStudyLogCommand {
        studyMinutes = studyMinutes == null ? 0 : studyMinutes;
    }
}
