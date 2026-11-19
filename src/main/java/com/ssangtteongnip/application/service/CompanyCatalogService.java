package com.ssangtteongnip.application.service;

import com.ssangtteongnip.domain.company.CompanyCatalogEntry;
import com.ssangtteongnip.domain.company.CompanyCatalogRepository;
import com.ssangtteongnip.domain.company.CompanyCatalogSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyCatalogService {

    private final CompanyCatalogRepository catalogRepository;
    private final CompanyCatalogSource catalogSource;

    // 목록에 없는 회사는 추가하고 이미 있는 회사는 최신 정보로 고친다. 새로 추가한 개수를 반환한다.
    @Transactional
    public int refresh() {
        Map<String, CompanyCatalogEntry> saved = catalogRepository.findAll().stream()
                .collect(Collectors.toMap(CompanyCatalogEntry::getCorpCode, Function.identity()));
        List<CompanyCatalogEntry> added = new ArrayList<>();
        for (CompanyCatalogEntry latest : catalogSource.findAll()) {
            if (!latest.isJobTarget()) {
                continue;
            }
            CompanyCatalogEntry existing = saved.get(latest.getCorpCode());
            if (existing == null) {
                added.add(latest);
            } else {
                existing.reviseInfo(latest);
            }
        }
        catalogRepository.saveAll(added);
        return added.size();
    }

    public boolean isEmpty() {
        return catalogRepository.count() == 0;
    }
}
