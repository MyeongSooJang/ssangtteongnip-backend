package com.ssangtteongnip.domain;

import com.ssangtteongnip.domain.company.CompanyCatalogEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyCatalogEntryTest {

    private CompanyCatalogEntry entry(String name) {
        return CompanyCatalogEntry.create("00126380", name, null, true);
    }

    @Test
    void 법인명과_영문명을_검색용으로_정규화해_저장한다() {
        CompanyCatalogEntry entry = CompanyCatalogEntry.create("00266961", "NAVER", "NAVER Corp.", true);

        assertThat(entry.getName()).isEqualTo("NAVER");
        assertThat(entry.getSearchName()).isEqualTo("naver");
        assertThat(entry.getSearchEngName()).isEqualTo("navercorp");
    }

    @Test
    void 영문명이_없으면_검색용_영문명도_없다() {
        CompanyCatalogEntry entry = CompanyCatalogEntry.create("00126380", "카카오", " ", false);

        assertThat(entry.getSearchEngName()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"(주)카카오", "㈜카카오", "주식회사 카카오", "카카오 ", "카 카 오"})
    void 법인_형태와_공백은_검색용_이름에서_제거된다(String name) {
        assertThat(entry(name).getSearchName()).isEqualTo("카카오");
    }

    @Test
    void 검색어의_와일드카드_문자는_제거된다() {
        assertThat(CompanyCatalogEntry.normalizeSearchText("카%카_오")).isEqualTo("카카오");
    }

    @Test
    void 지울_것만_있는_검색어는_빈_값이_된다() {
        assertThat(CompanyCatalogEntry.normalizeSearchText("(주) %")).isNull();
    }

    @Test
    void 전각_문자는_반각으로_맞춰_검색용_이름에_저장한다() {
        assertThat(entry("ＡＢＣ코리아").getSearchName()).isEqualTo("abc코리아");
    }

    @Test
    void 한글_영문_숫자가_없어_검색할_수_없는_이름은_취업_대상이_아니다() {
        assertThat(entry("株式會社").isJobTarget()).isFalse();
    }

    @Test
    void 정보를_고치면_검색용_이름도_함께_바뀐다() {
        CompanyCatalogEntry entry = entry("카카오");

        entry.reviseInfo("카카오뱅크", "KakaoBank", false);

        assertThat(entry.getSearchName()).isEqualTo("카카오뱅크");
        assertThat(entry.getSearchEngName()).isEqualTo("kakaobank");
        assertThat(entry.isListed()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "드림라인제일차유동화전문유한회사",
            "미래에셋네이버아시아그로쓰사모투자",
            "한국벤처투자조합",
            "케이비제12차"})
    void 금융_목적_법인은_취업_대상이_아니다(String name) {
        assertThat(entry(name).isJobTarget()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"카카오", "삼성전자", "우아한형제들"})
    void 일반_회사는_취업_대상이다(String name) {
        assertThat(entry(name).isJobTarget()).isTrue();
    }
}
