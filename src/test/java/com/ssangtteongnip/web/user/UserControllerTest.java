package com.ssangtteongnip.web.user;

import com.ssangtteongnip.application.service.UserService;
import com.ssangtteongnip.application.service.dto.NicknameResponse;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    private static final String BEARER = "Bearer valid-token";

    @Autowired MockMvc mockMvc;
    @MockitoBean TokenProvider tokenProvider;
    @MockitoBean UserService userService;

    @BeforeEach
    void setUp() {
        given(tokenProvider.parseUserId("valid-token")).willReturn(1L);
    }

    @Test
    void PATCH_me_nickname_닉네임을_바꾼다() throws Exception {
        mockMvc.perform(patch("/api/me/nickname")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"길동이\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userService).changeNickname(1L, "길동이");
    }

    @Test
    void PATCH_me_nickname_닉네임이_비어_있으면_400을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/me/nickname")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void PATCH_me_nickname_중복이면_409를_반환한다() throws Exception {
        willThrow(new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS))
                .given(userService).changeNickname(1L, "길동이");

        mockMvc.perform(patch("/api/me/nickname")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"길동이\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NICKNAME_ALREADY_EXISTS"));
    }

    @Test
    void PATCH_me_nickname_토큰이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"길동이\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void GET_me_nickname_내_닉네임을_반환한다() throws Exception {
        given(userService.findNickname(1L)).willReturn(new NicknameResponse("길동이"));

        mockMvc.perform(get("/api/me/nickname").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("길동이"));
    }
}
