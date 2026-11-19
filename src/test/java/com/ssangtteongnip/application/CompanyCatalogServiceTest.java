package com.ssangtteongnip.application;

import com.ssangtteongnip.application.service.CompanyCatalogService;
import com.ssangtteongnip.application.service.dto.CompanyCandidateResponse;
import com.ssangtteongnip.domain.company.Company;
import com.ssangtteongnip.domain.company.CompanyCatalogEntry;
import com.ssangtteongnip.domain.company.CompanyCatalogRepository;
import com.ssangtteongnip.domain.company.CompanyCatalogSource;
import com.ssangtteongnip.domain.company.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CompanyCatalogServiceTest {

    @Mock CompanyCatalogRepository catalogRepository;
    @Mock CompanyCatalogSource catalogSource;
    @Mock CompanyRepository companyRepository;

    @Captor ArgumentCaptor<Iterable<CompanyCatalogEntry>> savedCaptor;

    private CompanyCatalogService service() {
        return new CompanyCatalogService(catalogRepository, catalogSource, companyRepository);
    }

    private Company registeredCompany(Long id, String name) {
        Company company = Company.create(name, null, null);
        ReflectionTestUtils.setField(company, "id", id);
        return company;
    }

    @Test
    void 이미_등록된_회사는_회사_id와_함께_후보로_반환한다() {
        given(catalogRepository.findClosestTenMatches("카카오")).willReturn(List.of(
                CompanyCatalogEntry.create("00000001", "카카오", null, true),
                CompanyCatalogEntry.create("00000002", "카카오뱅크", null, true)));
        given(companyRepository.findByNameIn(List.of("카카오", "카카오뱅크")))
                .willReturn(List.of(registeredCompany(7L, "카카오")));

        List<CompanyCandidateResponse> candidates = service().findCandidates("카카오");

        assertThat(candidates).containsExactly(
                new CompanyCandidateResponse("카카오", true, 7L),
                new CompanyCandidateResponse("카카오뱅크", true, null));
    }

    @Test
    void 검색어는_법인_형태와_공백을_뺀_값으로_조회한다() {
        given(catalogRepository.findClosestTenMatches("카카오")).willReturn(List.of(
                CompanyCatalogEntry.create("00000001", "카카오", null, true)));
        given(companyRepository.findByNameIn(List.of("카카오"))).willReturn(List.of());

        List<CompanyCandidateResponse> candidates = service().findCandidates("(주) 카카오 ");

        assertThat(candidates).extracting(CompanyCandidateResponse::name).containsExactly("카카오");
    }

    @Test
    void 지울_것만_있는_검색어는_조회하지_않고_빈_목록을_반환한다() {
        List<CompanyCandidateResponse> candidates = service().findCandidates(" % ");

        assertThat(candidates).isEmpty();
        verifyNoInteractions(catalogRepository, companyRepository);
    }

    private CompanyCatalogEntry entry(String corpCode, String name) {
        return CompanyCatalogEntry.create(corpCode, name, null, false);
    }

    @Test
    void 목록에_없던_회사만_새로_저장한다() {
        given(catalogRepository.findAll()).willReturn(List.of(entry("00000001", "카카오")));
        given(catalogSource.findAll()).willReturn(List.of(entry("00000001", "카카오"), entry("00000002", "쿠팡")));

        int added = service().refresh();

        verify(catalogRepository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).extracting(CompanyCatalogEntry::getCorpCode).containsExactly("00000002");
        assertThat(added).isEqualTo(1);
    }

    @Test
    void 카탈로그가_비어_있는지_알려준다() {
        given(catalogRepository.count()).willReturn(0L);

        assertThat(service().isEmpty()).isTrue();
    }

    @Test
    void 카탈로그에_회사가_있으면_비어_있지_않다() {
        given(catalogRepository.count()).willReturn(3L);

        assertThat(service().isEmpty()).isFalse();
    }
}
