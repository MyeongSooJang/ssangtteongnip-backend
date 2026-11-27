package com.ssangtteongnip.application;

import com.ssangtteongnip.application.service.PostService;
import com.ssangtteongnip.application.service.dto.PostPageResponse;
import com.ssangtteongnip.application.service.dto.PostResponse;
import com.ssangtteongnip.application.service.request.PostRequest;
import com.ssangtteongnip.domain.FakeFileStorage;
import com.ssangtteongnip.domain.FakePasswordHasher;
import com.ssangtteongnip.domain.post.Post;
import com.ssangtteongnip.domain.post.PostImage;
import com.ssangtteongnip.domain.post.PostRepository;
import com.ssangtteongnip.domain.profile.PresignedUrl;
import com.ssangtteongnip.domain.user.Email;
import com.ssangtteongnip.domain.user.Nickname;
import com.ssangtteongnip.domain.user.User;
import com.ssangtteongnip.domain.user.UserRepository;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    private static final Long WRITER_ID = 1L;
    private static final Long OTHER_ID = 2L;
    private static final Long POST_ID = 10L;
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};

    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;

    private final FakeFileStorage storage = new FakeFileStorage();
    private PostService postService;
    private User writer;

    @BeforeEach
    void setUp() {
        postService = new PostService(postRepository, userRepository, storage);
        writer = User.register(new Email("hong@example.com"), "홍길동", new Nickname("길동이"), "password1",
                new FakePasswordHasher());
        ReflectionTestUtils.setField(writer, "id", WRITER_ID);
    }

    private void writerExists() {
        given(userRepository.findById(WRITER_ID)).willReturn(Optional.of(writer));
    }

    private Post existingPost(int imageCount) {
        List<byte[]> images = java.util.stream.IntStream.range(0, imageCount).mapToObj(i -> PNG).toList();
        Post post = Post.create(WRITER_ID, "면접 후기", "분위기가 좋았어요", images, storage);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        return post;
    }

    private Post lockedPost(int imageCount) {
        List<byte[]> images = java.util.stream.IntStream.range(0, imageCount).mapToObj(i -> PNG).toList();
        Post post = Post.create(WRITER_ID, "면접 후기", "분위기가 좋았어요", images, storage);
        given(postRepository.findByIdForUpdate(POST_ID)).willReturn(Optional.of(post));
        return post;
    }

    // ---- create ----

    @Test
    void 글을_쓰면_작성자_닉네임과_함께_돌려준다() {
        writerExists();
        given(postRepository.save(any(Post.class))).willAnswer(i -> i.getArgument(0));

        PostResponse response = postService.create(WRITER_ID, new PostRequest("제목", "내용", null, null));

        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.nickname()).isEqualTo("길동이");
        assertThat(response.mine()).isTrue();
    }

    @Test
    void 이미지를_함께_올리면_저장소에_올리고_공개_주소를_돌려준다() {
        writerExists();
        given(postRepository.save(any(Post.class))).willAnswer(i -> i.getArgument(0));

        PostResponse response = postService.create(WRITER_ID, new PostRequest("제목", "내용", null, List.of(PNG)));

        assertThat(storage.size()).isEqualTo(1);
        assertThat(response.images()).hasSize(1);
        assertThat(response.images().get(0).url()).startsWith("/api/post-images/");
    }

    @Test
    void 이미지와_남길_이미지를_생략하면_빈_목록으로_본다() {
        PostRequest request = new PostRequest("제목", "내용", null, null);

        assertThat(request.images()).isEmpty();
        assertThat(request.keepImageIds()).isEmpty();
    }

    @Test
    void 닉네임이_없으면_글을_쓸_수_없고_이미지도_올리지_않는다() {
        ReflectionTestUtils.setField(writer, "nickname", null);
        writerExists();

        assertErrorCode(() -> postService.create(WRITER_ID, new PostRequest("제목", "내용", null, List.of(PNG))),
                ErrorCode.NICKNAME_REQUIRED);

        verify(postRepository, never()).save(any());
        assertThat(storage.size()).isZero();
    }

    @Test
    void 없는_사용자는_글을_쓸_수_없다() {
        given(userRepository.findById(WRITER_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> postService.create(WRITER_ID, new PostRequest("제목", "내용", null, null)),
                ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void 이미지가_5장을_넘으면_저장소에도_DB에도_남기지_않는다() {
        writerExists();
        List<byte[]> six = java.util.stream.IntStream.range(0, 6).mapToObj(i -> PNG).toList();

        assertErrorCode(() -> postService.create(WRITER_ID, new PostRequest("제목", "내용", null, six)),
                ErrorCode.POST_IMAGE_LIMIT_EXCEEDED);

        assertThat(storage.size()).isZero();
        verify(postRepository, never()).save(any());
    }

    @Test
    void DB_저장이_실패하면_올린_이미지를_지우고_예외를_다시_던진다() {
        writerExists();
        given(postRepository.save(any(Post.class))).willThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> postService.create(WRITER_ID, new PostRequest("제목", "내용", null, List.of(PNG))))
                .isInstanceOf(IllegalStateException.class);

        assertThat(storage.size()).isZero();
    }

    // ---- find ----

    @Test
    void 글을_조회하면_작성자_여부를_알려준다() {
        Post post = existingPost(1);
        writerExists();

        PostResponse mine = postService.find(WRITER_ID, POST_ID);
        PostResponse others = postService.find(OTHER_ID, POST_ID);

        assertThat(mine.mine()).isTrue();
        assertThat(others.mine()).isFalse();
        assertThat(mine.nickname()).isEqualTo("길동이");
        assertThat(mine.images().get(0).id()).isEqualTo(post.getImages().get(0).getPublicId());
    }

    @Test
    void 없는_글은_조회할_수_없다() {
        given(postRepository.findById(POST_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> postService.find(WRITER_ID, POST_ID), ErrorCode.POST_NOT_FOUND);
    }

    // ---- findPage ----

    @Test
    void 목록은_최신글_20개씩_작성자_닉네임과_이미지_수를_담아_돌려준다() {
        Post post = Post.create(WRITER_ID, "면접 후기", "내용", List.of(PNG, PNG), storage);
        given(postRepository.findNewest(0, 20)).willReturn(List.of(post));
        given(postRepository.count()).willReturn(21L);
        given(userRepository.findAllById(any())).willReturn(List.of(writer));

        PostPageResponse response = postService.findPage(0);

        assertThat(response.pageSize()).isEqualTo(20);
        assertThat(response.totalCount()).isEqualTo(21L);
        assertThat(response.posts()).singleElement().satisfies(summary -> {
            assertThat(summary.title()).isEqualTo("면접 후기");
            assertThat(summary.nickname()).isEqualTo("길동이");
            assertThat(summary.imageCount()).isEqualTo(2);
        });
    }

    @Test
    void 글이_없으면_빈_목록을_돌려준다() {
        given(postRepository.findNewest(3, 20)).willReturn(List.of());
        given(postRepository.count()).willReturn(0L);
        given(userRepository.findAllById(any())).willReturn(List.of());

        PostPageResponse response = postService.findPage(3);

        assertThat(response.posts()).isEmpty();
        assertThat(response.page()).isEqualTo(3);
    }

    @Test
    void 페이지가_음수면_거절한다() {
        assertErrorCode(() -> postService.findPage(-1), ErrorCode.INVALID_REQUEST);

        verifyNoInteractions(postRepository);
    }

    // ---- rewrite ----

    @Test
    void 작성자가_아니면_수정할_수_없고_글은_그대로다() {
        Post post = lockedPost(0);

        assertErrorCode(() -> postService.rewrite(OTHER_ID, POST_ID, new PostRequest("새 제목", "새 내용", null, null)),
                ErrorCode.POST_NOT_WRITER);

        assertThat(post.getTitle()).isEqualTo("면접 후기");
    }

    @Test
    void 없는_글은_수정할_수_없다() {
        given(postRepository.findByIdForUpdate(POST_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> postService.rewrite(WRITER_ID, POST_ID, new PostRequest("제목", "내용", null, null)),
                ErrorCode.POST_NOT_FOUND);
    }

    @Test
    void 수정하면_빠진_이미지_객체는_지우고_남긴_이미지와_새_이미지는_둔다() {
        Post post = lockedPost(2);
        writerExists();
        PostImage kept = post.getImages().get(0);
        PostImage dropped = post.getImages().get(1);
        String droppedKey = dropped.getObjectKey();

        PostResponse response = postService.rewrite(WRITER_ID, POST_ID,
                new PostRequest("새 제목", "새 내용", Set.of(kept.getPublicId()), List.of(PNG)));

        assertThat(response.title()).isEqualTo("새 제목");
        assertThat(response.images()).hasSize(2);
        assertThat(storage.contains(droppedKey)).isFalse();
        assertThat(storage.contains(kept.getObjectKey())).isTrue();
        assertThat(storage.size()).isEqualTo(2);
    }

    @Test
    void 남긴_이미지와_새_이미지가_5장을_넘으면_수정하지_않는다() {
        Post post = lockedPost(3);
        Set<UUID> keepAll = Set.of(post.getImages().get(0).getPublicId(),
                post.getImages().get(1).getPublicId(), post.getImages().get(2).getPublicId());
        List<byte[]> three = List.of(PNG, PNG, PNG);

        assertErrorCode(() -> postService.rewrite(WRITER_ID, POST_ID, new PostRequest("새 제목", "내용", keepAll, three)),
                ErrorCode.POST_IMAGE_LIMIT_EXCEEDED);

        assertThat(post.getTitle()).isEqualTo("면접 후기");
        assertThat(storage.size()).isEqualTo(3);
    }

    @Test
    void 남의_글에_속한_이미지_아이디로는_남길_수_없다() {
        lockedPost(1);

        assertErrorCode(() -> postService.rewrite(WRITER_ID, POST_ID,
                        new PostRequest("제목", "내용", Set.of(UUID.randomUUID()), null)),
                ErrorCode.POST_IMAGE_NOT_FOUND);
    }

    // ---- delete ----

    @Test
    void 작성자가_아니면_삭제할_수_없고_이미지도_남는다() {
        lockedPost(1);

        assertErrorCode(() -> postService.delete(OTHER_ID, POST_ID), ErrorCode.POST_NOT_WRITER);

        verify(postRepository, never()).delete(any());
        assertThat(storage.size()).isEqualTo(1);
    }

    @Test
    void 삭제하면_글과_이미지_객체를_모두_지운다() {
        Post post = lockedPost(2);

        postService.delete(WRITER_ID, POST_ID);

        verify(postRepository).delete(post);
        assertThat(storage.size()).isZero();
    }

    @Test
    void 이미지_객체_삭제가_실패해도_글_삭제는_성공한다() {
        Post post = lockedPost(1);
        storage.failOnDelete();

        assertThatCode(() -> postService.delete(WRITER_ID, POST_ID)).doesNotThrowAnyException();

        verify(postRepository).delete(post);
    }

    @Test
    void 없는_글은_삭제할_수_없다() {
        given(postRepository.findByIdForUpdate(POST_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> postService.delete(WRITER_ID, POST_ID), ErrorCode.POST_NOT_FOUND);
    }

    // ---- 이미지 보기 ----

    @Test
    void 공개_아이디로_보여_줄_이미지_주소를_찾는다() {
        Post post = Post.create(WRITER_ID, "제목", "내용", List.of(PNG), storage);
        PostImage image = post.getImages().get(0);
        given(postRepository.findImage(image.getPublicId())).willReturn(Optional.of(image));

        PresignedUrl link = postService.findImageViewUrl(image.getPublicId());

        assertThat(link.url()).contains(image.getObjectKey());
    }

    @Test
    void 없는_이미지는_찾을_수_없다() {
        UUID unknown = UUID.randomUUID();
        given(postRepository.findImage(unknown)).willReturn(Optional.empty());

        assertErrorCode(() -> postService.findImageViewUrl(unknown), ErrorCode.POST_IMAGE_NOT_FOUND);
    }
}
