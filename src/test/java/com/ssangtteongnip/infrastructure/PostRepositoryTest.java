package com.ssangtteongnip.infrastructure;

import com.ssangtteongnip.domain.FakeFileStorage;
import com.ssangtteongnip.domain.post.Post;
import com.ssangtteongnip.domain.post.PostImage;
import com.ssangtteongnip.domain.post.PostRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PostRepositoryTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};

    @Autowired PostRepository postRepository;
    @Autowired EntityManager em;

    private final FakeFileStorage storage = new FakeFileStorage();

    private Post save(String title, int imageCount) {
        List<byte[]> images = java.util.stream.IntStream.range(0, imageCount).mapToObj(i -> PNG).toList();
        return postRepository.save(Post.create(1L, title, "내용", images, storage));
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Test
    void 글과_이미지를_저장하고_읽어온다() {
        Post saved = save("제목", 2);
        flushAndClear();

        Post found = postRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getTitle()).isEqualTo("제목");
        assertThat(found.getUserId()).isEqualTo(1L);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getImages()).hasSize(2);
        assertThat(found.getImages().get(0).getObjectKey()).startsWith("post-images/1/");
    }

    @Test
    void 최신_글부터_페이지_크기만큼_돌려준다() {
        Post first = save("첫째", 0);
        Post second = save("둘째", 0);
        Post third = save("셋째", 0);
        flushAndClear();

        assertThat(postRepository.findNewest(0, 2)).extracting(Post::getId)
                .containsExactly(third.getId(), second.getId());
        assertThat(postRepository.findNewest(1, 2)).extracting(Post::getId)
                .containsExactly(first.getId());
        assertThat(postRepository.findNewest(2, 2)).isEmpty();
    }

    @Test
    void 전체_글_수를_센다() {
        save("첫째", 0);
        save("둘째", 0);
        flushAndClear();

        assertThat(postRepository.count()).isEqualTo(2);
    }

    @Test
    void 공개_아이디로_이미지를_찾는다() {
        Post saved = save("제목", 1);
        UUID publicId = saved.getImages().get(0).getPublicId();
        flushAndClear();

        PostImage found = postRepository.findImage(publicId).orElseThrow();

        assertThat(found.getPublicId()).isEqualTo(publicId);
        assertThat(postRepository.findImage(UUID.randomUUID())).isEmpty();
    }

    @Test
    void 글을_지우면_이미지_행도_함께_지워진다() {
        Post saved = save("제목", 2);
        flushAndClear();

        postRepository.delete(postRepository.findById(saved.getId()).orElseThrow());
        flushAndClear();

        assertThat(postRepository.findById(saved.getId())).isEmpty();
        assertThat(em.createQuery("select count(i) from PostImage i", Long.class).getSingleResult()).isZero();
    }

    @Test
    void 수정에서_빠진_이미지_행은_지워진다() {
        Post saved = save("제목", 2);
        flushAndClear();

        Post found = postRepository.findById(saved.getId()).orElseThrow();
        found.rewrite("제목", "내용", java.util.Set.of(found.getImages().get(0).getPublicId()), List.of(), storage);
        flushAndClear();

        assertThat(em.createQuery("select count(i) from PostImage i", Long.class).getSingleResult()).isEqualTo(1L);
    }

    @Test
    void 잠그고_조회해도_글을_찾는다() {
        Post saved = save("제목", 0);
        flushAndClear();

        assertThat(postRepository.findByIdForUpdate(saved.getId())).isPresent();
    }
}
