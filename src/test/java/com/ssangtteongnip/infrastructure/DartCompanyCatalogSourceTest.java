package com.ssangtteongnip.infrastructure;

import com.ssangtteongnip.domain.company.CompanyCatalogEntry;
import com.ssangtteongnip.infrastructure.catalog.DartCompanyCatalogSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DartCompanyCatalogSourceTest {

    private static final String URL = "https://opendart.fss.or.kr/api/corpCode.xml?crtfc_key=test-key";

    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    private DartCompanyCatalogSource source(String apiKey) {
        return new DartCompanyCatalogSource(builder, apiKey);
    }

    private String list(String corpCode, String name, String engName, String stockCode, String modifyDate) {
        return """
                <list>
                    <corp_code>%s</corp_code>
                    <corp_name>%s</corp_name>
                    <corp_eng_name>%s</corp_eng_name>
                    <stock_code>%s</stock_code>
                    <modify_date>%s</modify_date>
                </list>
                """.formatted(corpCode, name, engName, stockCode, modifyDate);
    }

    private byte[] zip(String... lists) throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><result>" + String.join("", lists) + "</result>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("CORPCODE.xml"));
            zip.write(xml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    private void respond(byte[] body) {
        server.expect(requestTo(URL)).andRespond(withSuccess(body, MediaType.APPLICATION_OCTET_STREAM));
    }

    @Test
    void 받은_목록을_카탈로그_항목으로_바꾼다() throws Exception {
        respond(zip(list("00266961", "NAVER", "NAVER Corp.", "035420", "20240101")));

        List<CompanyCatalogEntry> entries = source("test-key").findAll();

        assertThat(entries).hasSize(1);
        CompanyCatalogEntry entry = entries.get(0);
        assertThat(entry.getCorpCode()).isEqualTo("00266961");
        assertThat(entry.getName()).isEqualTo("NAVER");
        assertThat(entry.getEngName()).isEqualTo("NAVER Corp.");
        assertThat(entry.isListed()).isTrue();
        server.verify();
    }

    @Test
    void 종목코드가_비어_있으면_비상장이고_영문명이_비어_있으면_영문명이_없다() throws Exception {
        respond(zip(list("00430964", "굿앤엘에스", " ", " ", "20170630")));

        CompanyCatalogEntry entry = source("test-key").findAll().get(0);

        assertThat(entry.isListed()).isFalse();
        assertThat(entry.getEngName()).isNull();
    }

    @Test
    void 같은_이름이_여러_번_나오면_상장사_하나만_남긴다() throws Exception {
        respond(zip(
                list("00000001", "카카오", "", " ", "20240101"),
                list("00000002", "카카오", "", "035720", "20170630")));

        List<CompanyCatalogEntry> entries = source("test-key").findAll();

        assertThat(entries).extracting(CompanyCatalogEntry::getCorpCode).containsExactly("00000002");
    }

    @Test
    void 같은_이름이_모두_비상장이면_최근에_수정된_것_하나만_남긴다() throws Exception {
        respond(zip(
                list("00000001", "다코", "", " ", "20170630"),
                list("00000002", "다코", "", " ", "20240101")));

        List<CompanyCatalogEntry> entries = source("test-key").findAll();

        assertThat(entries).extracting(CompanyCatalogEntry::getCorpCode).containsExactly("00000002");
    }

    @Test
    void 이름이_없는_항목은_건너뛴다() throws Exception {
        respond(zip(list("00000001", " ", "", " ", "20240101")));

        assertThat(source("test-key").findAll()).isEmpty();
    }

    @Test
    void 인증키가_없으면_요청하지_않고_빈_목록을_반환한다() {
        List<CompanyCatalogEntry> entries = source("").findAll();

        assertThat(entries).isEmpty();
        server.verify();
    }

    @Test
    void 인증키가_틀려_zip이_아닌_응답이_오면_예외를_던진다() {
        respond("<result><status>010</status><message>등록되지 않은 키입니다.</message></result>"
                .getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> source("test-key").findAll())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("zip이 아니다");
    }

    @Test
    void 요청이_실패하면_인증키가_없는_메시지의_예외를_던진다() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> source("test-key").findAll())
                .isInstanceOf(IllegalStateException.class)
                .hasNoCause()
                .message().doesNotContain("test-key");
    }
}
