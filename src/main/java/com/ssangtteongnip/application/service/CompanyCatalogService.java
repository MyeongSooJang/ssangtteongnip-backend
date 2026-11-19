package com.ssangtteongnip.application.service;

import com.ssangtteongnip.application.service.dto.CompanyCandidateResponse;
import com.ssangtteongnip.domain.company.Company;
import com.ssangtteongnip.domain.company.CompanyCatalog;
import com.ssangtteongnip.domain.company.CompanyCatalogEntry;
import com.ssangtteongnip.domain.company.CompanyCatalogRepository;
import com.ssangtteongnip.domain.company.CompanyCatalogSource;
import com.ssangtteongnip.domain.company.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        Map<String, Long> registeredIds = companyRepository.findByNameIn(names).stream()
                .collect(Collectors.toMap(Company::getName, Company::getId));
        return matches.stream()
                .map(entry -> CompanyCandidateResponse.attachCompanyId(entry, registeredIds.get(entry.getName())))
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
