package com.ssangtteongnip.domain;

import com.ssangtteongnip.domain.post.Post;
import com.ssangtteongnip.domain.post.PostImage;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class PostTest {

    private static final Long WRITER_ID = 1L;
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};

    private final FakeFileStorage storage = new FakeFileStorage();

    private Post writePost(List<byte[]> images) {
        return Post.create(WRITER_ID, "면접 후기", "분위기가 좋았어요", images, storage);
    }

    private static List<byte[]> pngs(int count) {
        return java.util.stream.IntStream.range(0, count).mapToObj(i -> PNG).toList();
    }

    @Test
    void 글을_쓰면_작성자와_제목_내용이_담긴다() {
        Post post = writePost(List.of());

        assertThat(post.getUserId()).isEqualTo(WRITER_ID);
        assertThat(post.getTitle()).isEqualTo("면접 후기");
        assertThat(post.getContent()).isEqualTo("분위기가 좋았어요");
        assertThat(post.countImages()).isZero();
    }

    @Test
    void 이미지를_첨부하면_글_작성자_폴더의_키로_저장소에_올라간다() {
        Post post = writePost(List.of(PNG));

        PostImage image = post.getImages().get(0);
        assertThat(image.getObjectKey()).startsWith("post-images/1/").endsWith(".png");
        assertThat(storage.contentOf(image.getObjectKey())).isEqualTo(PNG);
        assertThat(image.createPublicPath()).isEqualTo("/api/post-images/" + image.getPublicId());
    }

    @Test
    void 이미지는_5장까지_첨부할_수_있다() {
        Post post = writePost(pngs(5));

        assertThat(post.countImages()).isEqualTo(5);
        assertThat(storage.size()).isEqualTo(5);
    }

    @Test
    void 이미지가_5장을_넘으면_하나도_올리지_않고_거절한다() {
        assertErrorCode(() -> writePost(pngs(6)), ErrorCode.POST_IMAGE_LIMIT_EXCEEDED);

        assertThat(storage.size()).isZero();
    }

    @Test
    void 이미지가_아닌_파일이_섞여_있으면_이미_올린_이미지도_지운다() {
        assertErrorCode(() -> writePost(List.of(PNG, "text".getBytes())), ErrorCode.INVALID_IMAGE_TYPE);

        assertThat(storage.size()).isZero();
    }

    @Test
    void 비어_있는_이미지는_올릴_수_없다() {
        assertErrorCode(() -> writePost(List.of(new byte[0])), ErrorCode.EMPTY_FILE);
    }

    @Test
    void 이미지가_5MB를_넘으면_올릴_수_없다() {
        byte[] tooLarge = new byte[5 * 1024 * 1024 + 1];

        assertErrorCode(() -> writePost(List.of(tooLarge)), ErrorCode.FILE_TOO_LARGE);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void 제목이_비어_있으면_글을_쓸_수_없다(String title) {
        assertErrorCode(() -> Post.create(WRITER_ID, title, "내용", List.of(), storage), ErrorCode.POST_TITLE_REQUIRED);
    }

    @Test
    void 제목은_100자까지_쓸_수_있고_넘으면_거절한다() {
        assertThatCode(() -> Post.create(WRITER_ID, "가".repeat(100), "내용", List.of(), storage))
                .doesNotThrowAnyException();
        assertErrorCode(() -> Post.create(WRITER_ID, "가".repeat(101), "내용", List.of(), storage),
                ErrorCode.POST_TITLE_REQUIRED);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void 내용이_비어_있으면_글을_쓸_수_없다(String content) {
        assertErrorCode(() -> Post.create(WRITER_ID, "제목", content, List.of(), storage), ErrorCode.POST_CONTENT_REQUIRED);
    }

    @Test
    void 내용은_5000자까지_쓸_수_있고_넘으면_거절한다() {
        assertThatCode(() -> Post.create(WRITER_ID, "제목", "가".repeat(5000), List.of(), storage))
                .doesNotThrowAnyException();
        assertErrorCode(() -> Post.create(WRITER_ID, "제목", "가".repeat(5001), List.of(), storage),
                ErrorCode.POST_CONTENT_REQUIRED);
    }

    @Test
    void 제목이_틀리면_이미지를_올리지_않는다() {
        assertErrorCode(() -> Post.create(WRITER_ID, " ", "내용", List.of(PNG), storage), ErrorCode.POST_TITLE_REQUIRED);

        assertThat(storage.size()).isZero();
    }

    @Test
    void 작성자만_작성자로_확인된다() {
        Post post = writePost(List.of());

        assertThat(post.isWrittenBy(WRITER_ID)).isTrue();
        assertThat(post.isWrittenBy(2L)).isFalse();
        assertThatCode(() -> post.validateWrittenBy(WRITER_ID)).doesNotThrowAnyException();
        assertErrorCode(() -> post.validateWrittenBy(2L), ErrorCode.POST_NOT_WRITER);
    }

    @Test
    void 수정하면_제목과_내용이_바뀐다() {
        Post post = writePost(List.of());

        post.rewrite("새 제목", "새 내용", Set.of(), List.of(), storage);

        assertThat(post.getTitle()).isEqualTo("새 제목");
        assertThat(post.getContent()).isEqualTo("새 내용");
    }

    @Test
    void 이미지만_바꿔도_수정_시각이_기록된다() {
        Post post = writePost(List.of());
        assertThat(post.getUpdatedAt()).isNull();

        post.rewrite("면접 후기", "분위기가 좋았어요", Set.of(), List.of(PNG), storage);

        assertThat(post.getUpdatedAt()).isNotNull();
    }

    @Test
    void 수정이_실패하면_수정_시각은_그대로다() {
        Post post = writePost(pngs(3));
        Set<UUID> keepAll = Set.of(post.getImages().get(0).getPublicId(),
                post.getImages().get(1).getPublicId(), post.getImages().get(2).getPublicId());

        assertErrorCode(() -> post.rewrite("새 제목", "새 내용", keepAll, pngs(3), storage),
                ErrorCode.POST_IMAGE_LIMIT_EXCEEDED);

        assertThat(post.getUpdatedAt()).isNull();
    }

    @Test
    void 수정할_때_남기지_않은_이미지는_글에서_빠지고_돌려준다() {
        Post post = writePost(pngs(2));
        PostImage kept = post.getImages().get(0);
        PostImage dropped = post.getImages().get(1);

        List<PostImage> removed = post.rewrite("제목", "내용", Set.of(kept.getPublicId()), List.of(), storage);

        assertThat(removed).containsExactly(dropped);
        assertThat(post.getImages()).containsExactly(kept);
    }

    @Test
    void 수정할_때_새_이미지를_추가한다() {
        Post post = writePost(List.of(PNG));
        UUID keptId = post.getImages().get(0).getPublicId();

        post.rewrite("제목", "내용", Set.of(keptId), List.of(PNG, PNG), storage);

        assertThat(post.countImages()).isEqualTo(3);
        assertThat(storage.size()).isEqualTo(3);
    }

    @Test
    void 남긴_이미지와_새_이미지가_5장을_넘으면_거절하고_아무것도_바꾸지_않는다() {
        Post post = writePost(pngs(3));
        Set<UUID> keepAll = Set.of(post.getImages().get(0).getPublicId(),
                post.getImages().get(1).getPublicId(), post.getImages().get(2).getPublicId());

        assertErrorCode(() -> post.rewrite("새 제목", "새 내용", keepAll, pngs(3), storage),
                ErrorCode.POST_IMAGE_LIMIT_EXCEEDED);

        assertThat(post.getTitle()).isEqualTo("면접 후기");
        assertThat(post.countImages()).isEqualTo(3);
        assertThat(storage.size()).isEqualTo(3);
    }

    @Test
    void 이_글의_이미지가_아닌_것을_남기려_하면_거절한다() {
        Post post = writePost(List.of(PNG));

        assertErrorCode(() -> post.rewrite("제목", "내용", Set.of(UUID.randomUUID()), List.of(), storage),
                ErrorCode.POST_IMAGE_NOT_FOUND);
    }

    @Test
    void 수정할_때_새_이미지가_잘못되면_글은_그대로이고_올린_이미지는_지운다() {
        Post post = writePost(List.of(PNG));
        UUID keptId = post.getImages().get(0).getPublicId();

        assertErrorCode(() -> post.rewrite("새 제목", "새 내용", Set.of(keptId), List.of(PNG, "text".getBytes()), storage),
                ErrorCode.INVALID_IMAGE_TYPE);

        assertThat(post.getTitle()).isEqualTo("면접 후기");
        assertThat(post.countImages()).isEqualTo(1);
        assertThat(storage.size()).isEqualTo(1);
    }

    @Test
    void 이미지_객체를_모두_지운다() {
        Post post = writePost(pngs(2));

        post.deleteImageObjects(storage);

        assertThat(storage.size()).isZero();
    }

    @Test
    void 이미지_객체_삭제가_실패해도_예외로_번지지_않는다() {
        Post post = writePost(List.of(PNG));
        storage.failOnDelete();

        assertThatCode(() -> post.deleteImageObjects(storage)).doesNotThrowAnyException();
    }
}
