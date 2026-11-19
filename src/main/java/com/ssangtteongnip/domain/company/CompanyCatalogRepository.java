package com.ssangtteongnip.domain.company;

import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CompanyCatalogRepository {
    List<CompanyCatalogEntry> findAll();
    <S extends CompanyCatalogEntry> List<S> saveAll(Iterable<S> entries);

    // keyword는 CompanyCatalogEntry.normalizeSearchText로 정규화한 값이어야 한다.
    // 정확 일치, 접두어 일치, 부분 일치 순이고 같은 순위에서는 상장사, 짧은 이름 순이다.
    List<CompanyCatalogEntry> findClosestMatches(String keyword, Pageable pageable);
}
