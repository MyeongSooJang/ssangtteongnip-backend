package com.ssangtteongnip.application.service.command;

public record RewriteStudyLogCommand(
        String title,
        String content,
        int studyMinutes
) {}
