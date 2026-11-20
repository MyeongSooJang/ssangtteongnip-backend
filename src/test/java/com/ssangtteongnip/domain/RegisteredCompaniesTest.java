package com.ssangtteongnip.domain;

import com.ssangtteongnip.domain.company.Company;
import com.ssangtteongnip.domain.company.CompanyCatalogEntry;
import com.ssangtteongnip.domain.company.RegisteredCompanies;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RegisteredCompaniesTest {

    private Company registeredCompany(Long id, String name) {
        Company company = Company.create(name, null, null);
        ReflectionTestUtils.setField(company, "id", id);
        return company;
    }

    private CompanyCatalogEntry catalogEntry(String name) {
        return CompanyCatalogEntry.create("00000001", name, null, true);
    }

    @Test
    void 이름이_같은_회사가_등록되어_있으면_회사_id를_찾는다() {
        RegisteredCompanies registered = new RegisteredCompanies(List.of(registeredCompany(7L, "카카오")));

        assertThat(registered.findCompanyId(catalogEntry("카카오"))).isEqualTo(7L);
    }

    @Test
    void 등록되지_않은_회사는_회사_id가_없다() {
        RegisteredCompanies registered = new RegisteredCompanies(List.of(registeredCompany(7L, "카카오")));

        assertThat(registered.findCompanyId(catalogEntry("쿠팡"))).isNull();
    }
}
