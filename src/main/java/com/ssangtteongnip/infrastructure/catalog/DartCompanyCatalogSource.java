package com.ssangtteongnip.infrastructure.catalog;

import com.ssangtteongnip.domain.company.CompanyCatalogEntry;
import com.ssangtteongnip.domain.company.CompanyCatalogSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

// 금융감독원 OpenDART의 기업 고유번호 목록(zip 안의 CORPCODE.xml)을 받아 온다.
// 인증키는 URL 쿼리로 보내야 해서 RestClientException 메시지에 키가 들어갈 수 있다. 예외를 로그나 cause로 잇지 않는다.
@Slf4j
@Component
public class DartCompanyCatalogSource implements CompanyCatalogSource {

    static final String CORP_CODE_URL = "https://opendart.fss.or.kr/api/corpCode.xml?crtfc_key={key}";

    private final RestClient restClient;
    private final String apiKey;

    public DartCompanyCatalogSource(RestClient.Builder restClientBuilder,
                                    @Value("${app.dart.api-key:}") String apiKey) {
        this.restClient = restClientBuilder.build();
        this.apiKey = apiKey;
    }

    @Override
    public List<CompanyCatalogEntry> findAll() {
        if (apiKey.isBlank()) {
            log.warn("DART_API_KEY가 없어 기업 목록을 받지 않는다");
            return List.of();
        }
        List<Row> rows = parse(unzip(download()));
        return keepBestPerName(rows).stream()
                .map(Row::createEntry)
                .toList();
    }

    private byte[] download() {
        try {
            byte[] body = restClient.get().uri(CORP_CODE_URL, apiKey).retrieve().body(byte[].class);
            if (body == null) {
                throw new IllegalStateException("DART 기업 목록 응답이 비어 있다");
            }
            return body;
        } catch (RestClientException e) {
            log.warn("DART 기업 목록 요청 실패 {}", e.getClass().getSimpleName());
            throw new IllegalStateException("DART 기업 목록을 받지 못했다");
        }
    }

    // 인증키가 틀리면 zip이 아니라 오류 XML이 200으로 온다. 그 경우 zip 항목이 없다.
    private byte[] unzip(byte[] body) {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(body))) {
            if (zip.getNextEntry() == null) {
                throw new IllegalStateException("DART 응답이 기업 목록 zip이 아니다");
            }
            return zip.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("DART 기업 목록 zip을 읽지 못했다", e);
        }
    }

    private List<Row> parse(byte[] xml) {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        List<Row> rows = new ArrayList<>();
        try (InputStream in = new ByteArrayInputStream(xml)) {
            XMLStreamReader reader = factory.createXMLStreamReader(in);
            Map<String, String> fields = new HashMap<>();
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String element = reader.getLocalName();
                    if (element.equals("list")) {
                        fields.clear();
                    } else if (!element.equals("result")) {
                        fields.put(element, reader.getElementText().trim());
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT && reader.getLocalName().equals("list")) {
                    rows.add(Row.readFields(fields));
                }
            }
        } catch (XMLStreamException | IOException e) {
            throw new IllegalStateException("DART 기업 목록 XML을 읽지 못했다", e);
        }
        return rows;
    }

    // 같은 이름이 여러 번 나오면(예: 카카오) 상장사, 그다음 최근에 수정된 것 하나만 남긴다.
    private List<Row> keepBestPerName(List<Row> rows) {
        Map<String, Row> best = new LinkedHashMap<>();
        for (Row row : rows) {
            if (row.name().isEmpty()) {
                continue;
            }
            best.merge(row.name(), row, (kept, other) -> other.isBetterThan(kept) ? other : kept);
        }
        return List.copyOf(best.values());
    }

    private record Row(String corpCode, String name, String engName, String stockCode, String modifyDate) {

        static Row readFields(Map<String, String> fields) {
            return new Row(
                    fields.getOrDefault("corp_code", ""),
                    fields.getOrDefault("corp_name", ""),
                    fields.getOrDefault("corp_eng_name", ""),
                    fields.getOrDefault("stock_code", ""),
                    fields.getOrDefault("modify_date", ""));
        }

        boolean isListed() {
            return !stockCode.isEmpty();
        }

        boolean isBetterThan(Row other) {
            if (isListed() != other.isListed()) {
                return isListed();
            }
            return modifyDate.compareTo(other.modifyDate) > 0;
        }

        CompanyCatalogEntry createEntry() {
            return CompanyCatalogEntry.create(corpCode, name, engName.isEmpty() ? null : engName, isListed());
        }
    }
}
