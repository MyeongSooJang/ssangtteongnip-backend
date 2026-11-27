package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.post.Post;
import com.ssangtteongnip.domain.user.User;

import java.time.LocalDateTime;

public record PostSummaryResponse(Long id, String title, String nickname, int imageCount, LocalDateTime createdAt) {

    public static PostSummaryResponse convert(Post post, User author) {
        return new PostSummaryResponse(post.getId(), post.getTitle(), author.getNickname().value(),
                post.countImages(), post.getCreatedAt());
    }
}
