package com.ssangtteongnip.web.post;

import com.ssangtteongnip.application.service.PostService;
import com.ssangtteongnip.application.service.dto.PostImageResponse;
import com.ssangtteongnip.application.service.dto.PostPageResponse;
import com.ssangtteongnip.application.service.dto.PostResponse;
import com.ssangtteongnip.application.service.dto.PostSummaryResponse;
import com.ssangtteongnip.application.service.request.PostRequest;
import com.ssangtteongnip.domain.user.TokenProvider;
import com.ssangtteongnip.global.config.SecurityConfig;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostController.class)
@Import(SecurityConfig.class)
class PostControllerTest {

    private static final String BEARER = "Bearer valid-token";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 27, 10, 0);

    @Autowired MockMvc mockMvc;
    @MockitoBean TokenProvider tokenProvider;
    @MockitoBean PostService postService;

    @BeforeEach
    void setUp() {
        given(tokenProvider.parseUserId("valid-token")).willReturn(1L);
    }

    private static MockMultipartFile image() {
        return new MockMultipartFile("images", "a.png", "image/png", new byte[]{1, 2, 3});
    }

    private static PostResponse response(UUID imageId) {
        List<PostImageResponse> images = imageId == null ? List.of()
                : List.of(new PostImageResponse(imageId, "/api/post-images/" + imageId));
        return new PostResponse(5L, "제목", "내용", "길동이", true, images, NOW, NOW);
    }

    // ---- 작성 ----

    @Test
    void POST_posts_글을_등록하면_201과_글을_반환한다() throws Exception {
        UUID imageId = UUID.randomUUID();
        given(postService.create(eq(1L), any(PostRequest.class))).willReturn(response(imageId));

        mockMvc.perform(multipart("/api/posts").file(image())
                        .param("title", "제목").param("content", "내용")
                        .header("Authorization", BEARER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nickname").value("길동이"))
                .andExpect(jsonPath("$.data.mine").value(true))
                .andExpect(jsonPath("$.data.images[0].url").value("/api/post-images/" + imageId));

        ArgumentCaptor<PostRequest> captor = ArgumentCaptor.forClass(PostRequest.class);
        verify(postService).create(eq(1L), captor.capture());
        assertThat(captor.getValue().title()).isEqualTo("제목");
        assertThat(captor.getValue().images()).hasSize(1);
        assertThat(captor.getValue().images().get(0)).containsExactly(1, 2, 3);
        assertThat(captor.getValue().keepImageIds()).isEmpty();
    }

    @Test
    void POST_posts_이미지_없이도_등록할_수_있다() throws Exception {
        given(postService.create(eq(1L), any(PostRequest.class))).willReturn(response(null));

        mockMvc.perform(multipart("/api/posts")
                        .param("title", "제목").param("content", "내용")
                        .header("Authorization", BEARER))
                .andExpect(status().isCreated());

        ArgumentCaptor<PostRequest> captor = ArgumentCaptor.forClass(PostRequest.class);
        verify(postService).create(eq(1L), captor.capture());
        assertThat(captor.getValue().images()).isEmpty();
    }

    @Test
    void POST_posts_제목이_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(multipart("/api/posts").param("content", "내용").header("Authorization", BEARER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void POST_posts_이미지가_너무_많으면_400을_반환한다() throws Exception {
        given(postService.create(eq(1L), any(PostRequest.class)))
                .willThrow(new BusinessException(ErrorCode.POST_IMAGE_LIMIT_EXCEEDED));

        mockMvc.perform(multipart("/api/posts").file(image())
                        .param("title", "제목").param("content", "내용")
                        .header("Authorization", BEARER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("POST_IMAGE_LIMIT_EXCEEDED"));
    }

    @Test
    void POST_posts_닉네임이_없으면_400을_반환한다() throws Exception {
        given(postService.create(eq(1L), any(PostRequest.class)))
                .willThrow(new BusinessException(ErrorCode.NICKNAME_REQUIRED));

        mockMvc.perform(multipart("/api/posts")
                        .param("title", "제목").param("content", "내용")
                        .header("Authorization", BEARER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NICKNAME_REQUIRED"));
    }

    @Test
    void POST_posts_토큰이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(multipart("/api/posts").param("title", "제목").param("content", "내용"))
                .andExpect(status().isUnauthorized());
    }

    // ---- 목록·상세 ----

    @Test
    void GET_posts_페이지를_생략하면_첫_페이지를_반환한다() throws Exception {
        given(postService.findPage(0)).willReturn(new PostPageResponse(
                List.of(new PostSummaryResponse(5L, "제목", "길동이", 2, NOW)), 0, 20, 1));

        mockMvc.perform(get("/api/posts").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.posts[0].title").value("제목"))
                .andExpect(jsonPath("$.data.posts[0].imageCount").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.totalCount").value(1));
    }

    @Test
    void GET_posts_페이지가_숫자가_아니면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/posts").param("page", "abc").header("Authorization", BEARER))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_posts_토큰이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/posts")).andExpect(status().isUnauthorized());
    }

    @Test
    void GET_posts_id_글을_반환한다() throws Exception {
        given(postService.find(1L, 5L)).willReturn(response(null));

        mockMvc.perform(get("/api/posts/5").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.content").value("내용"));
    }

    @Test
    void GET_posts_id_없는_글은_404를_반환한다() throws Exception {
        given(postService.find(1L, 5L)).willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND));

        mockMvc.perform(get("/api/posts/5").header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    // ---- 수정·삭제 ----

    @Test
    void PUT_posts_id_수정하면_남길_이미지와_새_이미지를_함께_넘긴다() throws Exception {
        UUID keepId = UUID.randomUUID();
        given(postService.rewrite(eq(1L), eq(5L), any(PostRequest.class))).willReturn(response(keepId));

        mockMvc.perform(multipart("/api/posts/5").file(image())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .param("title", "새 제목").param("content", "새 내용")
                        .param("keepImageIds", keepId.toString())
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<PostRequest> captor = ArgumentCaptor.forClass(PostRequest.class);
        verify(postService).rewrite(eq(1L), eq(5L), captor.capture());
        assertThat(captor.getValue().title()).isEqualTo("새 제목");
        assertThat(captor.getValue().keepImageIds()).containsExactly(keepId);
        assertThat(captor.getValue().images()).hasSize(1);
    }

    @Test
    void PUT_posts_id_작성자가_아니면_403을_반환한다() throws Exception {
        given(postService.rewrite(eq(1L), eq(5L), any(PostRequest.class)))
                .willThrow(new BusinessException(ErrorCode.POST_NOT_WRITER));

        mockMvc.perform(multipart("/api/posts/5")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .param("title", "새 제목").param("content", "새 내용")
                        .header("Authorization", BEARER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POST_NOT_WRITER"));
    }

    @Test
    void DELETE_posts_id_삭제한다() throws Exception {
        mockMvc.perform(delete("/api/posts/5").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(postService).delete(1L, 5L);
    }

    @Test
    void DELETE_posts_id_작성자가_아니면_403을_반환한다() throws Exception {
        org.mockito.BDDMockito.willThrow(new BusinessException(ErrorCode.POST_NOT_WRITER))
                .given(postService).delete(1L, 5L);

        mockMvc.perform(delete("/api/posts/5").header("Authorization", BEARER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POST_NOT_WRITER"));
    }
}
