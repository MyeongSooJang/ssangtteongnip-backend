package com.ssangtteongnip.web.profile;

import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import org.mockito.ArgumentCaptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssangtteongnip.application.service.ProfileService;
import com.ssangtteongnip.application.service.dto.ProfileResponse;
import com.ssangtteongnip.application.service.dto.UserFileResponse;
import com.ssangtteongnip.application.service.request.ProfileRequest;
import com.ssangtteongnip.domain.profile.FileType;
import com.ssangtteongnip.domain.profile.PersonalInfo;
import com.ssangtteongnip.domain.user.TokenProvider;
import com.ssangtteongnip.global.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileController.class)
@Import(SecurityConfig.class)
class ProfileControllerTest {

    private static final String BEARER = "Bearer valid-token";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean TokenProvider tokenProvider;
    @MockitoBean ProfileService profileService;

    @BeforeEach
    void setUp() {
        given(tokenProvider.parseUserId("valid-token")).willReturn(1L);
    }

    private static ProfileResponse profileResponse(List<UserFileResponse> files) {
        return new ProfileResponse("hong@example.com", "홍길동", "010-1234-5678", null, null,
                null, null, null, null, null, "백엔드 개발자", null, LocalDate.of(2025, 3, 1), files);
    }

    @Test
    void GET_me_profile_내_프로필과_파일_목록을_종류와_함께_조회한다() throws Exception {
        UserFileResponse file = new UserFileResponse(3L, FileType.RESUME, "이력서.pdf", 1024,
                LocalDateTime.of(2026, 11, 1, 10, 0));
        given(profileService.getMyProfile(1L)).willReturn(profileResponse(List.of(file)));

        mockMvc.perform(get("/api/me/profile").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("hong@example.com"))
                .andExpect(jsonPath("$.data.desiredJob").value("백엔드 개발자"))
                .andExpect(jsonPath("$.data.jobSearchStartDate").value("2025-03-01"))
                .andExpect(jsonPath("$.data.files[0].id").value(3))
                .andExpect(jsonPath("$.data.files[0].type").value("RESUME"))
                .andExpect(jsonPath("$.data.files[0].fileName").value("이력서.pdf"))
                .andExpect(jsonPath("$.data.files[0].sizeBytes").value(1024));
    }

    @Test
    void GET_me_profile_파일이_없으면_files가_빈_배열이다() throws Exception {
        given(profileService.getMyProfile(1L)).willReturn(profileResponse(List.of()));

        mockMvc.perform(get("/api/me/profile").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.files").isEmpty());
    }

    @Test
    void GET_me_profile_토큰이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/me/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
    }

    @Test
    void GET_me_profile_유효하지_않은_토큰이면_401을_반환한다() throws Exception {
        given(tokenProvider.parseUserId("bad-token"))
                .willThrow(new com.ssangtteongnip.global.exception.BusinessException(
                        com.ssangtteongnip.global.exception.ErrorCode.UNAUTHORIZED));

        mockMvc.perform(get("/api/me/profile").header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
    }

    @Test
    void PUT_me_profile_인적_사항을_저장한다() throws Exception {
        given(profileService.reviseMyProfile(eq(1L), any())).willReturn(profileResponse(null));

        mockMvc.perform(put("/api/me/profile")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "홍길동", "phone", "010-1234-5678", "desiredJob", "백엔드 개발자"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("홍길동"));
    }

    @Test
    void PUT_me_profile_이름이_비어_있으면_400을_반환한다() throws Exception {
        mockMvc.perform(put("/api/me/profile")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", " "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void PUT_me_profile_취업_준비_시작일을_저장한다() throws Exception {
        given(profileService.reviseMyProfile(eq(1L), any())).willReturn(profileResponse(null));

        mockMvc.perform(put("/api/me/profile")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "홍길동", "jobSearchStartDate", "2025-03-01"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobSearchStartDate").value("2025-03-01"));
    }

    @Test
    void PUT_me_profile_취업_준비_시작일이_미래이면_400을_반환한다() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(put("/api/me/profile")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "홍길동", "jobSearchStartDate", tomorrow))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void PUT_me_profile_취업_준비_시작일이_오늘이면_저장한다() throws Exception {
        given(profileService.reviseMyProfile(eq(1L), any())).willReturn(profileResponse(null));

        mockMvc.perform(put("/api/me/profile")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "홍길동", "jobSearchStartDate", LocalDate.now().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void PUT_me_profile_취업_준비_시작일을_생략해도_저장한다() throws Exception {
        given(profileService.reviseMyProfile(eq(1L), any())).willReturn(profileResponse(null));

        mockMvc.perform(put("/api/me/profile")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "홍길동"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void PUT_me_profile_인적_사항_필드를_구분해_서비스에_전달한다() throws Exception {
        given(profileService.reviseMyProfile(eq(1L), any())).willReturn(profileResponse(null));

        mockMvc.perform(put("/api/me/profile")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.ofEntries(
                                Map.entry("name", "홍길동"),
                                Map.entry("phone", "010-1234-5678"),
                                Map.entry("birthDate", "1998-04-27"),
                                Map.entry("address", "서울시"),
                                Map.entry("githubUrl", "https://github.com/hong"),
                                Map.entry("blogUrl", "https://blog.example.com"),
                                Map.entry("portfolioUrl", "https://portfolio.example.com"),
                                Map.entry("education", "OO대 컴퓨터공학"),
                                Map.entry("career", "인턴 6개월"),
                                Map.entry("desiredJob", "백엔드 개발자"),
                                Map.entry("introduction", "안녕하세요"),
                                Map.entry("jobSearchStartDate", "2025-03-01")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<ProfileRequest> captor = ArgumentCaptor.forClass(ProfileRequest.class);
        verify(profileService).reviseMyProfile(eq(1L), captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("홍길동");
        assertThat(captor.getValue().toPersonalInfo()).usingRecursiveComparison().isEqualTo(PersonalInfo.of(
                "010-1234-5678", LocalDate.of(1998, 4, 27), "서울시",
                "https://github.com/hong", "https://blog.example.com", "https://portfolio.example.com",
                "OO대 컴퓨터공학", "인턴 6개월", "백엔드 개발자", "안녕하세요",
                LocalDate.of(2025, 3, 1)));
    }
}
