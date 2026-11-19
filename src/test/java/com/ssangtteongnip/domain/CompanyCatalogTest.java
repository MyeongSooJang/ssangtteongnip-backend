package com.ssangtteongnip.domain;

import com.ssangtteongnip.domain.company.CompanyCatalog;
import com.ssangtteongnip.domain.company.CompanyCatalogEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyCatalogTest {

    private CompanyCatalogEntry entry(String corpCode, String name) {
        return CompanyCatalogEntry.create(corpCode, name, null, false);
    }

    @Test
    void 목록에_없던_회사만_새로_저장할_회사로_반환한다() {
        CompanyCatalog catalog = new CompanyCatalog(List.of(entry("00000001", "카카오")));

        List<CompanyCatalogEntry> added = catalog.refreshWith(
                List.of(entry("00000001", "카카오"), entry("00000002", "쿠팡")));

        assertThat(added).extracting(CompanyCatalogEntry::getCorpCode).containsExactly("00000002");
    }

    @Test
    void 이미_있는_회사는_최신_정보로_고친다() {
        CompanyCatalogEntry saved = entry("00000001", "카카오");
        CompanyCatalog catalog = new CompanyCatalog(List.of(saved));

        List<CompanyCatalogEntry> added = catalog.refreshWith(List.of(
                CompanyCatalogEntry.create("00000001", "카카오", "Kakao Corp.", true)));

        assertThat(saved.isListed()).isTrue();
        assertThat(saved.getSearchEngName()).isEqualTo("kakaocorp");
        assertThat(added).isEmpty();
    }

    @Test
    void 금융_목적_법인은_저장할_회사에서_뺀다() {
        CompanyCatalog catalog = new CompanyCatalog(List.of());

        List<CompanyCatalogEntry> added = catalog.refreshWith(List.of(
                entry("00000001", "드림라인제일차유동화전문유한회사"), entry("00000002", "쿠팡")));

        assertThat(added).extracting(CompanyCatalogEntry::getName).containsExactly("쿠팡");
    }

    @Test
    void 같은_고유번호가_목록에_두_번_있어도_한_번만_저장할_회사로_반환한다() {
        CompanyCatalog catalog = new CompanyCatalog(List.of());

        List<CompanyCatalogEntry> added = catalog.refreshWith(List.of(
                entry("00000001", "카카오"),
                CompanyCatalogEntry.create("00000001", "카카오", null, true)));

        assertThat(added).hasSize(1);
        assertThat(added.get(0).isListed()).isTrue();
    }
}
