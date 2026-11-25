package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.plan.MissReason;
import com.ssangtteongnip.domain.plan.Plan;
import com.ssangtteongnip.domain.plan.PlanCategory;
import com.ssangtteongnip.domain.plan.PlanDetail;
import com.ssangtteongnip.domain.plan.PlanStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PlanResponse(Long id, LocalDate planDate, String content, PlanCategory category, String doneCriteria,
                           int estimatedMinutes, String timeSlot, String place, String firstAction,
                           PlanStatus status, Integer actualMinutes, MissReason missReason, String memo,
                           LocalDateTime completedAt, List<PlanEvidenceResponse> evidences) {

    public static PlanResponse convert(Plan plan) {
        PlanDetail detail = plan.getDetail();
        return new PlanResponse(plan.getId(), plan.getPlanDate(), detail.getContent(), detail.getCategory(),
                detail.getDoneCriteria(), detail.getEstimatedMinutes(), detail.getTimeSlot(), detail.getPlace(),
                detail.getFirstAction(), plan.getStatus(), plan.getActualMinutes(), plan.getMissReason(),
                plan.getMemo(), plan.getCompletedAt(),
                plan.getEvidences().stream().map(PlanEvidenceResponse::convert).toList());
    }
}
