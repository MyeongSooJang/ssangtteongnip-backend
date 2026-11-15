package com.ssangtteongnip.application.service.command;

public record RewriteStudyLogCommand(
        String title,
        String content,
        Integer studyMinutes
) {
    // 공부 시간을 생략하면 0분으로 저장한다.
    public RewriteStudyLogCommand {
        studyMinutes = studyMinutes == null ? 0 : studyMinutes;
    }
}
