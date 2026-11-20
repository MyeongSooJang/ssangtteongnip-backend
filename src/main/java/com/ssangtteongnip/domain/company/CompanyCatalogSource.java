package com.ssangtteongnip.domain.company;

import java.util.List;

public interface CompanyCatalogSource {
    // 공개 기업 목록을 모두 받아 온다. 받지 못하면 IllegalStateException을 던진다.
    List<CompanyCatalogEntry> findAll();
}
