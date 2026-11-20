package com.ssangtteongnip.infrastructure;

import com.ssangtteongnip.domain.company.CompanyCatalogEntry;
import com.ssangtteongnip.global.config.JpaConfig;
import com.ssangtteongnip.infrastructure.persistence.CompanyCatalogJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class CompanyCatalogRepositoryTest {

    @Autowired CompanyCatalogJpaRepository catalogRepository;

    private int corpCodeSeq = 0;

    private void save(String name, String engName, boolean listed) {
        String corpCode = String.format("%08d", ++corpCodeSeq);
        catalogRepository.saveAndFlush(CompanyCatalogEntry.create(corpCode, name, engName, listed));
    }

    private void saveSameKeywordCompanies(String keyword, int count) {
        for (int i = 0; i < count; i++) {
            save(keyword + i, null, true);
        }
    }

    private List<String> search(String keyword) {
        return catalogRepository.findClosestTenMatches(CompanyCatalogEntry.normalizeSearchText(keyword))
                .stream().map(CompanyCatalogEntry::getName).toList();
    }

    @Test
    void 정확_일치가_접두어_일치보다_접두어_일치가_부분_일치보다_먼저_나온다() {
        save("대한카카오", null, true);
        save("카카오뱅크", null, true);
        save("카카오", null, false);

        assertThat(search("카카오")).containsExactly("카카오", "카카오뱅크", "대한카카오");
    }

    @Test
    void 같은_순위에서는_상장사가_먼저_나온다() {
        save("카카오페이", null, false);
        save("카카오뱅크", null, true);

        assertThat(search("카카오")).containsExactly("카카오뱅크", "카카오페이");
    }

    @Test
    void 같은_순위와_상장_여부면_짧은_이름이_먼저_나온다() {
        save("카카오엔터테인먼트", null, true);
        save("카카오게임", null, true);

        assertThat(search("카카오")).containsExactly("카카오게임", "카카오엔터테인먼트");
    }

    @Test
    void 영문명으로도_찾는다() {
        save("NAVER", "NAVER Corp.", true);
        save("삼성전자", "SAMSUNG ELECTRONICS CO.,LTD", true);

        assertThat(search("naver")).containsExactly("NAVER");
    }

    @Test
    void 법인_형태와_공백은_무시하고_찾는다() {
        save("주식회사 우아한형제들", null, false);

        assertThat(search("(주) 우아한")).containsExactly("주식회사 우아한형제들");
    }

    @Test
    void 후보는_최대_10건까지만_반환한다() {
        saveSameKeywordCompanies("카카오", 12);

        assertThat(search("카카오")).hasSize(10);
    }

    @Test
    void 일치하는_회사가_없으면_빈_목록이다() {
        save("카카오", null, true);

        assertThat(search("쿠팡")).isEmpty();
    }
}
