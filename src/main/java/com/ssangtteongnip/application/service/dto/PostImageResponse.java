package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.post.PostImage;

import java.util.UUID;

public record PostImageResponse(UUID id, String url) {

    public static PostImageResponse convert(PostImage image) {
        return new PostImageResponse(image.getPublicId(), image.createPublicPath());
    }
}
