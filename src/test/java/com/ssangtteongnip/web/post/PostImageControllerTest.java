package com.ssangtteongnip.web.post;

import com.ssangtteongnip.application.service.PostService;
import com.ssangtteongnip.domain.profile.PresignedUrl;
import com.ssangtteongnip.domain.user.TokenProvider;
import com.ssangtteongnip.global.config.SecurityConfig;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostImageController.class)
@Import(SecurityConfig.class)
class PostImageControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean TokenProvider tokenProvider;
    @MockitoBean PostService postService;

    @Test
    void GET_post_images_토큰이_없어도_저장소_주소로_302_리다이렉트한다() throws Exception {
        UUID id = UUID.randomUUID();
        given(postService.findImageViewUrl(id))
                .willReturn(new PresignedUrl("https://bucket.example/post-images/1/a.png?sig=1", Instant.now()));

        mockMvc.perform(get("/api/post-images/" + id))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://bucket.example/post-images/1/a.png?sig=1"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=60, private"));
    }

    @Test
    void GET_post_images_없는_이미지는_404를_반환한다() throws Exception {
        UUID id = UUID.randomUUID();
        given(postService.findImageViewUrl(id)).willThrow(new BusinessException(ErrorCode.POST_IMAGE_NOT_FOUND));

        mockMvc.perform(get("/api/post-images/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_IMAGE_NOT_FOUND"));
    }

    @Test
    void GET_post_images_형식이_다른_아이디는_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/post-images/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_post_images_토큰이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/post-images/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void PUT_post_images_토큰이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(put("/api/post-images/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void DELETE_post_images_토큰이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(delete("/api/post-images/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}
