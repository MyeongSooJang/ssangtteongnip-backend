package com.ssangtteongnip.domain.company;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 저장된 회사 카탈로그. 받아 온 최신 기업 목록을 반영하는 규칙을 가진다.
public class CompanyCatalog {

    private final Map<String, CompanyCatalogEntry> entryByCorpCode = new HashMap<>();

    public CompanyCatalog(List<CompanyCatalogEntry> savedEntries) {
        for (CompanyCatalogEntry saved : savedEntries) {
            entryByCorpCode.put(saved.getCorpCode(), saved);
        }
    }

    // 취업 대상이 아닌 회사는 건너뛰고, 이미 있는 회사는 최신 정보로 고친다. 새로 저장해야 할 회사만 반환한다.
    // 같은 고유번호가 목록에 두 번 있으면 먼저 나온 새 회사를 고치므로 중복 저장되지 않는다.
    public List<CompanyCatalogEntry> refreshWith(List<CompanyCatalogEntry> latestEntries) {
        List<CompanyCatalogEntry> added = new ArrayList<>();
        for (CompanyCatalogEntry latest : latestEntries) {
            if (!latest.isJobTarget()) {
                continue;
            }
            CompanyCatalogEntry known = entryByCorpCode.get(latest.getCorpCode());
            if (known == null) {
                entryByCorpCode.put(latest.getCorpCode(), latest);
                added.add(latest);
            } else {
                known.reviseInfo(latest);
            }
        }
        return added;
    }
}
