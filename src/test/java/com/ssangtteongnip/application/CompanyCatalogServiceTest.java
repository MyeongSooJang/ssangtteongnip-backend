package com.ssangtteongnip.application;

import com.ssangtteongnip.application.service.CompanyCatalogService;
import com.ssangtteongnip.domain.company.CompanyCatalogEntry;
import com.ssangtteongnip.domain.company.CompanyCatalogRepository;
import com.ssangtteongnip.domain.company.CompanyCatalogSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CompanyCatalogServiceTest {

    @Mock CompanyCatalogRepository catalogRepository;
    @Mock CompanyCatalogSource catalogSource;

    @Captor ArgumentCaptor<Iterable<CompanyCatalogEntry>> savedCaptor;

    private CompanyCatalogService service() {
        return new CompanyCatalogService(catalogRepository, catalogSource);
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
    void 이미_있는_회사는_최신_정보로_고친다() {
        CompanyCatalogEntry saved = entry("00000001", "카카오");
        given(catalogRepository.findAll()).willReturn(List.of(saved));
        given(catalogSource.findAll()).willReturn(List.of(
                CompanyCatalogEntry.create("00000001", "카카오", "Kakao Corp.", true)));

        int added = service().refresh();

        assertThat(saved.isListed()).isTrue();
        assertThat(saved.getSearchEngName()).isEqualTo("kakaocorp");
        assertThat(added).isZero();
    }

    @Test
    void 금융_목적_법인은_저장하지_않는다() {
        given(catalogRepository.findAll()).willReturn(List.of());
        given(catalogSource.findAll()).willReturn(List.of(
                entry("00000001", "드림라인제일차유동화전문유한회사"), entry("00000002", "쿠팡")));

        int added = service().refresh();

        verify(catalogRepository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).extracting(CompanyCatalogEntry::getName).containsExactly("쿠팡");
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
