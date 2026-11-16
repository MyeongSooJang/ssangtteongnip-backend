package com.ssangtteongnip.global.exception;

import com.ssangtteongnip.application.service.StudyLogService;
import com.ssangtteongnip.domain.user.TokenProvider;
import com.ssangtteongnip.global.config.SecurityConfig;
import com.ssangtteongnip.web.studylog.StudyLogController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudyLogController.class)
@Import(SecurityConfig.class)
class GlobalExceptionHandlerTest {

    private static final String BEARER = "Bearer valid-token";
    private static final String SECRET_MESSAGE = "내부 DB 접속 정보 노출 금지";

    @Autowired MockMvc mockMvc;
    @MockitoBean TokenProvider tokenProvider;
    @MockitoBean StudyLogService studyLogService;

    @BeforeEach
    void setUp() {
        given(tokenProvider.parseUserId("valid-token")).willReturn(1L);
    }

    @Test
    void 깨진_JSON_본문이면_400_INVALID_REQUEST를_반환한다() throws Exception {
        mockMvc.perform(post("/api/me/study-logs")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{깨진 json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_REQUEST.getMessage()));
    }

    @Test
    void 경로_값_타입이_맞지_않으면_400_INVALID_REQUEST를_반환한다() throws Exception {
        mockMvc.perform(get("/api/me/study-logs/abc")
                        .header("Authorization", BEARER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_REQUEST.getMessage()));
    }

    @Test
    void 쿼리_파라미터_형식이_틀리면_400_INVALID_REQUEST를_반환한다() throws Exception {
        mockMvc.perform(get("/api/me/study-logs")
                        .header("Authorization", BEARER)
                        .param("from", "FOO")
                        .param("to", "2026-11-03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_REQUEST.getMessage()));
    }

    @Test
    void 필수_파라미터가_없으면_400_INVALID_REQUEST를_반환한다() throws Exception {
        mockMvc.perform(get("/api/me/study-logs")
                        .header("Authorization", BEARER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_REQUEST.getMessage()));
    }

    @Test
    void 지원하지_않는_메서드면_405_METHOD_NOT_ALLOWED를_반환한다() throws Exception {
        mockMvc.perform(patch("/api/me/study-logs/1")
                        .header("Authorization", BEARER))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value(ErrorCode.METHOD_NOT_ALLOWED.getMessage()));
    }

    @Test
    void 없는_경로면_404_RESOURCE_NOT_FOUND를_반환한다() throws Exception {
        mockMvc.perform(get("/api/me/없는-경로")
                        .header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(ErrorCode.RESOURCE_NOT_FOUND.getMessage()));
    }

    @Test
    void 예상하지_못한_예외면_500을_반환하고_예외_메시지를_노출하지_않는다() throws Exception {
        given(studyLogService.find(1L, 10L)).willThrow(new RuntimeException(SECRET_MESSAGE));

        mockMvc.perform(get("/api/me/study-logs/10")
                        .header("Authorization", BEARER))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_ERROR.getMessage()))
                .andExpect(content().string(not(containsString(SECRET_MESSAGE))));
    }

    @Test
    void 비즈니스_예외는_catch_all이_가로채지_않고_원래_상태코드로_반환한다() throws Exception {
        given(studyLogService.find(1L, 10L)).willThrow(new BusinessException(ErrorCode.STUDY_LOG_NOT_FOUND));

        mockMvc.perform(get("/api/me/study-logs/10")
                        .header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY_LOG_NOT_FOUND"));
    }
}
