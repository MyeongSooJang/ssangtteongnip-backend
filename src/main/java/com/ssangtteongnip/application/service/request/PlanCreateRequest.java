package com.ssangtteongnip.application.service.request;

import com.ssangtteongnip.domain.plan.PlanCategory;
import com.ssangtteongnip.domain.plan.PlanDetail;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PlanCreateRequest(
        @NotNull(message = "계획 날짜를 입력해주세요.") LocalDate planDate,
        @Size(max = 50, message = "계획 내용은 50자 이하여야 합니다.") String content,
        PlanCategory category,
        @Size(max = 200, message = "완료 기준은 200자 이하여야 합니다.") String doneCriteria,
        @NotNull(message = "목표 시간을 입력해주세요.") Integer estimatedMinutes,
        @Size(max = 100, message = "시간대는 100자 이하여야 합니다.") String timeSlot,
        @Size(max = 100, message = "장소는 100자 이하여야 합니다.") String place,
        @Size(max = 100, message = "첫 행동은 100자 이하여야 합니다.") String firstAction) {

    public PlanDetail createDetail() {
        return new PlanDetail(content, category, doneCriteria, estimatedMinutes, timeSlot, place, firstAction);
    }
}
