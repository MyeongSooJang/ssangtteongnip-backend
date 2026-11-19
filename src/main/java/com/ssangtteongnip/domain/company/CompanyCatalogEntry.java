package com.ssangtteongnip.domain.company;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Locale;
import java.util.regex.Pattern;

// 회사 이름 자동완성을 위한 공개 기업 목록(DART)의 한 줄이다. 사용자가 등록한 회사(Company)와는 별개다.
@Entity
@Table(name = "company_catalog")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyCatalogEntry {

    private static final Pattern FINANCIAL_VEHICLE = Pattern.compile("유동화전문|사모투자|투자조합|제\\d+차");
    private static final Pattern CORPORATE_FORM = Pattern.compile("\\(주\\)|㈜|주식회사");
    private static final Pattern NON_SEARCHABLE = Pattern.compile("[^0-9a-z가-힣]");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 8)
    private String corpCode;

    @Column(nullable = false)
    private String name;

    private String engName;

    @Column(nullable = false)
    private String searchName;

    private String searchEngName;

    @Column(nullable = false)
    private boolean listed;

    public static CompanyCatalogEntry create(String corpCode, String name, String engName, boolean listed) {
        CompanyCatalogEntry entry = new CompanyCatalogEntry();
        entry.corpCode = corpCode;
        entry.reviseInfo(name, engName, listed);
        return entry;
    }

    public void reviseInfo(String name, String engName, boolean listed) {
        this.name = name;
        this.engName = engName;
        this.searchName = normalizeSearchText(name);
        this.searchEngName = normalizeSearchText(engName);
        this.listed = listed;
    }

    // 유동화전문회사·사모펀드처럼 취업 대상이 아닌 금융 목적 법인인지 판단한다.
    public boolean isJobTarget() {
        return !FINANCIAL_VEHICLE.matcher(name).find();
    }

    // 법인 형태, 공백, 기호를 지우고 소문자로 맞춘다. 사용자가 입력한 검색어도 같은 규칙으로 맞춘다.
    // 지원하지 않는 문자(%, _ 포함)가 지워지므로 LIKE 검색어에 그대로 쓸 수 있다.
    public static String normalizeSearchText(String text) {
        if (text == null) {
            return null;
        }
        String withoutForm = CORPORATE_FORM.matcher(text.toLowerCase(Locale.ROOT)).replaceAll("");
        String normalized = NON_SEARCHABLE.matcher(withoutForm).replaceAll("");
        return normalized.isEmpty() ? null : normalized;
    }
}
