package com.ssangtteongnip.application.service;

import com.ssangtteongnip.application.service.dto.CompanyCandidateResponse;
import com.ssangtteongnip.domain.company.CompanyCatalog;
import com.ssangtteongnip.domain.company.CompanyCatalogEntry;
import com.ssangtteongnip.domain.company.CompanyCatalogRepository;
import com.ssangtteongnip.domain.company.CompanyCatalogSource;
import com.ssangtteongnip.domain.company.CompanyRepository;
import com.ssangtteongnip.domain.company.RegisteredCompanies;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyCatalogService {

    private static final int MAX_CANDIDATES = 10;

    private final CompanyCatalogRepository catalogRepository;
    private final CompanyCatalogSource catalogSource;
    private final CompanyRepository companyRepository;

    // 입력한 글자와 가까운 순으로 후보를 준다. 이미 등록된 회사는 companyId를 함께 준다.
    public List<CompanyCandidateResponse> findCandidates(String keyword) {
        String searchText = CompanyCatalogEntry.normalizeSearchText(keyword);
        if (searchText == null) {
            return List.of();
        }
        List<CompanyCatalogEntry> matches = catalogRepository.findClosestMatches(
                searchText, PageRequest.of(0, MAX_CANDIDATES));
        List<String> names = matches.stream().map(CompanyCatalogEntry::getName).toList();
        RegisteredCompanies registered = new RegisteredCompanies(companyRepository.findByNameIn(names));
        return matches.stream()
                .map(entry -> {
                    Long companyId = registered.findCompanyId(entry);
                    return CompanyCandidateResponse.attachCompanyId(entry, companyId);
                })
                .toList();
    }

    // 새로 추가한 회사 수를 반환한다.
    @Transactional
    public int refresh() {
        CompanyCatalog catalog = new CompanyCatalog(catalogRepository.findAll());
        List<CompanyCatalogEntry> added = catalog.refreshWith(catalogSource.findAll());
        catalogRepository.saveAll(added);
        return added.size();
    }

    public boolean isEmpty() {
        return catalogRepository.count() == 0;
    }
}
