package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.post.Post;
import com.ssangtteongnip.domain.user.User;

import java.time.LocalDateTime;
import java.util.List;

// mine은 보는 사람이 작성자인지 알려 주어 프런트가 수정·삭제 버튼을 보일지 정하게 한다.
public record PostResponse(Long id, String title, String content, String nickname, boolean mine,
                           List<PostImageResponse> images, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static PostResponse convert(Post post, User author, Long viewerId) {
        return new PostResponse(post.getId(), post.getTitle(), post.getContent(),
                author.getNickname().value(), post.isWrittenBy(viewerId),
                post.getImages().stream().map(PostImageResponse::convert).toList(),
                post.getCreatedAt(), post.getUpdatedAt());
    }
}
