package com.ssangtteongnip.domain.post;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository {
    Post save(Post post);
    void delete(Post post);
    void flush();
    Optional<Post> findById(Long id);

    // 이미지 개수 한도를 동시 수정이 함께 넘지 못하도록 글 행을 잠그고 조회한다.
    Optional<Post> findByIdForUpdate(Long id);

    // 최신 글부터. page는 0부터 센다. 도메인은 Spring Data의 Pageable을 쓰지 않으므로 숫자 둘로 받는다.
    List<Post> findNewest(int page, int size);

    long count();

    // 어느 글의 이미지든 공개 아이디로 찾는다(이미지 보기 주소용).
    Optional<PostImage> findImage(UUID publicId);
}
