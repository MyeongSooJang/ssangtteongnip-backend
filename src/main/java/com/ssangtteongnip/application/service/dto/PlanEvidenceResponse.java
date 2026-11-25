package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.plan.EvidenceType;
import com.ssangtteongnip.domain.plan.PlanEvidence;

public record PlanEvidenceResponse(Long id, EvidenceType type, String url, String fileName, Long sizeBytes) {

    public static PlanEvidenceResponse convert(PlanEvidence evidence) {
        return new PlanEvidenceResponse(evidence.getId(), evidence.getType(), evidence.getUrl(),
                evidence.getFileName(), evidence.getSizeBytes());
    }
}
