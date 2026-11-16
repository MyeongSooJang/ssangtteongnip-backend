package com.ssangtteongnip.global.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 데이터가_있는_성공_응답은_success와_data만_직렬화한다() throws Exception {
        JsonNode json = objectMapper.valueToTree(ApiResponse.ok("값"));

        assertThat(json.get("success").asBoolean()).isTrue();
        assertThat(json.get("data").asText()).isEqualTo("값");
        assertThat(json.has("code")).isFalse();
        assertThat(json.has("message")).isFalse();
    }

    @Test
    void 본문_없는_성공_응답은_success만_직렬화한다() throws Exception {
        JsonNode json = objectMapper.valueToTree(ApiResponse.ok());

        assertThat(json.get("success").asBoolean()).isTrue();
        assertThat(json.has("data")).isFalse();
        assertThat(json.has("code")).isFalse();
        assertThat(json.has("message")).isFalse();
    }

    @Test
    void 오류_코드로_만든_실패_응답은_코드명과_메시지를_담는다() throws Exception {
        JsonNode json = objectMapper.valueToTree(ApiResponse.fail(ErrorCode.UNAUTHORIZED));

        assertThat(json.get("success").asBoolean()).isFalse();
        assertThat(json.get("code").asText()).isEqualTo("UNAUTHORIZED");
        assertThat(json.get("message").asText()).isEqualTo(ErrorCode.UNAUTHORIZED.getMessage());
        assertThat(json.has("data")).isFalse();
    }

    @Test
    void 코드와_메시지로_만든_실패_응답은_그_값을_담는다() throws Exception {
        JsonNode json = objectMapper.valueToTree(ApiResponse.fail("VALIDATION_ERROR", "잘못된 입력"));

        assertThat(json.get("success").asBoolean()).isFalse();
        assertThat(json.get("code").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(json.get("message").asText()).isEqualTo("잘못된 입력");
    }
}
