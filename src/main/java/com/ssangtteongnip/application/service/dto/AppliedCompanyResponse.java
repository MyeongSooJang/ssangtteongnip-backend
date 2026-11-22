package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.company.Company;

import java.time.LocalDateTime;

// 지원·일정·후기 응답 안에 담기는 지원한 회사. 후기 수 같은 통계는 담지 않는다.
public record AppliedCompanyResponse(Long id, String name, String industry, String website, LocalDateTime createdAt) {

    public static AppliedCompanyResponse convert(Company company) {
        return new AppliedCompanyResponse(company.getId(), company.getName(), company.getIndustry(),
                company.getWebsite(), company.getCreatedAt());
    }
}
