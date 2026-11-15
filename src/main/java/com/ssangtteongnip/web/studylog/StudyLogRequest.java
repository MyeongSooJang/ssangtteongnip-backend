package com.ssangtteongnip.web.studylog;

import com.ssangtteongnip.application.service.command.RewriteStudyLogCommand;
import jakarta.validation.constraints.Size;

public record StudyLogRequest(
        @Size(max = 100, message = "제목은 100자 이하여야 합니다.") String title,
        @Size(max = 10000, message = "공부 내용은 10000자 이하여야 합니다.") String content,
        Integer studyMinutes) {

    public RewriteStudyLogCommand toCommand() {
        return new RewriteStudyLogCommand(title, content, studyMinutes);
    }
}
