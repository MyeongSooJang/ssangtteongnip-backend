package com.ssangtteongnip.domain.company;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 이미 등록된 회사 모음. 카탈로그 회사와 이름이 같은 회사를 같은 회사로 본다.
public class RegisteredCompanies {

    private final Map<String, Long> companyIdByName = new HashMap<>();

    public RegisteredCompanies(List<Company> companies) {
        for (Company company : companies) {
            companyIdByName.put(company.getName(), company.getId());
        }
    }

    // 등록된 회사면 회사 id를, 아직 등록되지 않았으면 null을 반환한다.
    public Long findCompanyId(CompanyCatalogEntry entry) {
        return companyIdByName.get(entry.getName());
    }
}
