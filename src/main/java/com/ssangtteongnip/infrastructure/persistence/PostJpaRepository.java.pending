package com.ssangtteongnip.infrastructure.persistence;

import com.ssangtteongnip.domain.post.Post;
import com.ssangtteongnip.domain.post.PostImage;
import com.ssangtteongnip.domain.post.PostRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostJpaRepository extends JpaRepository<Post, Long>, PostRepository {

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p where p.id = :id")
    Optional<Post> findByIdForUpdate(@Param("id") Long id);

    @Query("select p from Post p order by p.id desc")
    List<Post> findNewestPosts(Pageable pageable);

    @Override
    default List<Post> findNewest(int page, int size) {
        return findNewestPosts(PageRequest.of(page, size));
    }

    @Override
    @Query("select i from PostImage i where i.publicId = :publicId")
    Optional<PostImage> findImage(@Param("publicId") UUID publicId);
}
