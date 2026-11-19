package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.company.CompanyCatalogEntry;

// companyId가 있으면 이미 등록된 회사이고, 없으면 카탈로그에만 있는 회사라 선택할 때 등록해야 한다.
public record CompanyCandidateResponse(String name, boolean listed, Long companyId) {

    public static CompanyCandidateResponse attachCompanyId(CompanyCatalogEntry entry, Long companyId) {
        return new CompanyCandidateResponse(entry.getName(), entry.isListed(), companyId);
    }
}
