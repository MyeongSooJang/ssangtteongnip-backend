package com.ssangtteongnip.application.service.request;

import com.ssangtteongnip.domain.plan.MissReason;
import jakarta.validation.constraints.Size;

// 값이 없는(null) 항목은 바꾸지 않는다. 메모만 빈 문자열로 보내 지울 수 있다.
public record PlanReflectionRequest(
        Integer actualMinutes,
        MissReason missReason,
        @Size(max = 200, message = "메모는 200자 이하여야 합니다.") String memo) {
}
