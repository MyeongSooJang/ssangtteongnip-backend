package com.ssangtteongnip.web.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssangtteongnip.application.service.PlanService;
import com.ssangtteongnip.application.service.dto.FileDownload;
import com.ssangtteongnip.application.service.dto.PlanEvidenceResponse;
import com.ssangtteongnip.application.service.dto.PlanResponse;
import com.ssangtteongnip.application.service.request.PlanCompleteRequest;
import com.ssangtteongnip.application.service.request.PlanCreateRequest;
import com.ssangtteongnip.application.service.request.PlanDetailRequest;
import com.ssangtteongnip.application.service.request.PlanReflectionRequest;
import com.ssangtteongnip.domain.plan.EvidenceType;
import com.ssangtteongnip.domain.plan.MissReason;
import com.ssangtteongnip.domain.plan.PlanCategory;
import com.ssangtteongnip.domain.plan.PlanStatus;
import com.ssangtteongnip.domain.user.TokenProvider;
import com.ssangtteongnip.global.config.SecurityConfig;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlanController.class)
@Import(SecurityConfig.class)
class PlanControllerTest {

    private static final String BEARER = "Bearer valid-token";
    private static final LocalDate DATE = LocalDate.of(2026, 11, 26);

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean TokenProvider tokenProvider;
    @MockitoBean PlanService planService;

    @BeforeEach
    void setUp() {
        given(tokenProvider.parseUserId("valid-token")).willReturn(1L);
    }

    private PlanResponse response() {
        return new PlanResponse(10L, DATE, "이력서 수정", PlanCategory.APPLY, "PDF로 저장한다", 60, null, null, null,
                PlanStatus.PLANNED, null, null, null, null, List.of());
    }

    private Map<String, Object> createBody() {
        return Map.of("planDate", "2026-11-26", "content", "이력서 수정", "category", "APPLY",
                "doneCriteria", "PDF로 저장한다", "estimatedMinutes", 60);
    }

    @Test
    void POST_계획을_만들고_201과_만든_계획을_반환한다() throws Exception {
        given(planService.create(1L, new PlanCreateRequest(DATE, "이력서 수정", PlanCategory.APPLY, "PDF로 저장한다", 60,
                null, null, null))).willReturn(response());

        mockMvc.perform(post("/api/me/plans")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.status").value("PLANNED"))
                .andExpect(jsonPath("$.data.category").value("APPLY"))
                .andExpect(jsonPath("$.data.estimatedMinutes").value(60));
    }

    @Test
    void POST_날짜가_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/me/plans")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", "내용", "category", "APPLY", "doneCriteria", "기준", "estimatedMinutes", 30))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("계획 날짜를 입력해주세요."));
        verifyNoInteractions(planService);
    }

    @Test
    void POST_내용이_50자를_넘으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/me/plans")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "planDate", "2026-11-26", "content", "가".repeat(51), "category", "APPLY",
                                "doneCriteria", "기준", "estimatedMinutes", 30))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("계획 내용은 50자 이하여야 합니다."));
    }

    @Test
    void POST_목표_시간이_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/me/plans")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "planDate", "2026-11-26", "content", "내용", "category", "APPLY", "doneCriteria", "기준"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("목표 시간을 입력해주세요."));
    }

    @Test
    void POST_도메인_규칙을_어기면_서비스의_오류_코드로_400을_반환한다() throws Exception {
        given(planService.create(1L, new PlanCreateRequest(DATE, "이력서 수정", PlanCategory.APPLY, "PDF로 저장한다", 60,
                null, null, null))).willThrow(new BusinessException(ErrorCode.INVALID_PLAN_DATE));

        mockMvc.perform(post("/api/me/plans")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PLAN_DATE"));
    }

    @Test
    void POST_토큰이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/me/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody())))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(planService);
    }

    @Test
    void PATCH_계획을_고치고_200을_반환한다() throws Exception {
        given(planService.revise(1L, 10L, new PlanDetailRequest("이력서 수정", PlanCategory.APPLY, "PDF로 저장한다", 60,
                null, null, null))).willReturn(response());

        mockMvc.perform(patch("/api/me/plans/10")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", "이력서 수정", "category", "APPLY", "doneCriteria", "PDF로 저장한다",
                                "estimatedMinutes", 60))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10));
    }

    @Test
    void PATCH_다른_사용자의_계획이면_404를_반환한다() throws Exception {
        given(planService.revise(1L, 99L, new PlanDetailRequest("내용", PlanCategory.APPLY, "기준", 30, null, null, null)))
                .willThrow(new BusinessException(ErrorCode.PLAN_NOT_FOUND));

        mockMvc.perform(patch("/api/me/plans/99")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", "내용", "category", "APPLY", "doneCriteria", "기준", "estimatedMinutes", 30))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLAN_NOT_FOUND"));
    }

    @Test
    void POST_완료하면_200과_완료된_계획을_반환한다() throws Exception {
        given(planService.complete(1L, 10L, new PlanCompleteRequest(45, "집중"))).willReturn(response());

        mockMvc.perform(post("/api/me/plans/10/complete")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("actualMinutes", 45, "memo", "집중"))))
                .andExpect(status().isOk());
        verify(planService).complete(1L, 10L, new PlanCompleteRequest(45, "집중"));
    }

    @Test
    void POST_완료할_때_실제_시간이_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/me/plans/10/complete")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("실제 걸린 시간을 입력해주세요."));
        verifyNoInteractions(planService);
    }

    @Test
    void POST_결과물이_없어_완료할_수_없으면_서비스의_오류_코드로_400을_반환한다() throws Exception {
        given(planService.complete(1L, 10L, new PlanCompleteRequest(45, null)))
                .willThrow(new BusinessException(ErrorCode.PLAN_EVIDENCE_REQUIRED));

        mockMvc.perform(post("/api/me/plans/10/complete")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("actualMinutes", 45))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PLAN_EVIDENCE_REQUIRED"));
    }

    @Test
    void PATCH_기록을_수정하고_200을_반환한다() throws Exception {
        given(planService.reflect(1L, 10L, new PlanReflectionRequest(20, MissReason.TOO_BIG, "너무 컸다")))
                .willReturn(response());

        mockMvc.perform(patch("/api/me/plans/10/reflection")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "actualMinutes", 20, "missReason", "TOO_BIG", "memo", "너무 컸다"))))
                .andExpect(status().isOk());
        verify(planService).reflect(1L, 10L, new PlanReflectionRequest(20, MissReason.TOO_BIG, "너무 컸다"));
    }

    @Test
    void PATCH_알_수_없는_실패_사유면_400을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/me/plans/10/reflection")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("missReason", "LAZY"))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(planService);
    }

    @Test
    void DELETE_계획을_삭제하고_200을_반환한다() throws Exception {
        mockMvc.perform(delete("/api/me/plans/10").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(planService).delete(1L, 10L);
    }

    @Test
    void GET_기간으로_계획_목록을_조회한다() throws Exception {
        given(planService.findRange(1L, DATE, DATE.plusDays(6))).willReturn(List.of(response()));

        mockMvc.perform(get("/api/me/plans")
                        .header("Authorization", BEARER)
                        .param("from", "2026-11-26").param("to", "2026-12-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(10));
    }

    @Test
    void GET_기간이_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/me/plans").header("Authorization", BEARER))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(planService);
    }

    @Test
    void POST_링크_결과물을_추가하고_201을_반환한다() throws Exception {
        given(planService.addLink(1L, 10L, "https://github.com/a/b"))
                .willReturn(new PlanEvidenceResponse(5L, EvidenceType.URL, "https://github.com/a/b", null, null));

        mockMvc.perform(post("/api/me/plans/10/evidences")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("url", "https://github.com/a/b"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.type").value("URL"));
    }

    @Test
    void POST_링크가_500자를_넘으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/me/plans/10/evidences")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("url", "https://a.com/" + "a".repeat(500)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("링크는 500자 이하여야 합니다."));
        verifyNoInteractions(planService);
    }

    @Test
    void POST_파일_결과물을_올리고_201을_반환한다() throws Exception {
        byte[] content = "%PDF-1.7\nbody".getBytes(StandardCharsets.US_ASCII);
        given(planService.addFile(1L, 10L, "결과.pdf", content))
                .willReturn(new PlanEvidenceResponse(6L, EvidenceType.FILE, null, "결과.pdf", (long) content.length));

        mockMvc.perform(multipart("/api/me/plans/10/evidences")
                        .file(new MockMultipartFile("file", "결과.pdf", MediaType.APPLICATION_PDF_VALUE, content))
                        .header("Authorization", BEARER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("FILE"))
                .andExpect(jsonPath("$.data.fileName").value("결과.pdf"));
    }

    @Test
    void DELETE_결과물을_삭제하고_200을_반환한다() throws Exception {
        mockMvc.perform(delete("/api/me/plans/10/evidences/5").header("Authorization", BEARER))
                .andExpect(status().isOk());
        verify(planService).removeEvidence(1L, 10L, 5L);
    }

    @Test
    void GET_파일_결과물의_내려받기_주소를_반환한다() throws Exception {
        given(planService.findEvidenceDownload(1L, 10L, 6L))
                .willReturn(new FileDownload("https://storage/a.pdf", "결과.pdf", Instant.parse("2026-11-25T03:05:00Z")));

        mockMvc.perform(get("/api/me/plans/10/evidences/6/download").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value("https://storage/a.pdf"))
                .andExpect(jsonPath("$.data.fileName").value("결과.pdf"));
    }
}
