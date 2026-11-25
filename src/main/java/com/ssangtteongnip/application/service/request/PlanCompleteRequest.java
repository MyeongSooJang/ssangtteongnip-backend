package com.ssangtteongnip.application.service.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlanCompleteRequest(
        @NotNull(message = "실제 걸린 시간을 입력해주세요.") Integer actualMinutes,
        @Size(max = 200, message = "메모는 200자 이하여야 합니다.") String memo) {
}
