package com.ssangtteongnip.application.service.request;

import jakarta.validation.constraints.Size;

public record StudyLogRequest(
        @Size(max = 100, message = "제목은 100자 이하여야 합니다.") String title,
        @Size(max = 10000, message = "공부 내용은 10000자 이하여야 합니다.") String content,
        Integer studyMinutes) {

    // 공부 시간을 생략하면 0분으로 저장한다.
    public StudyLogRequest {
        studyMinutes = studyMinutes == null ? 0 : studyMinutes;
    }
}
