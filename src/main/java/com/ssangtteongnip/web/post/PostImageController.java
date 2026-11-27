package com.ssangtteongnip.web.post;

import com.ssangtteongnip.application.service.PostService;
import com.ssangtteongnip.domain.profile.PresignedUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/post-images")
@RequiredArgsConstructor
public class PostImageController {

    // 리다이렉트를 브라우저가 잠깐 재사용하되, presigned URL이 만료되기 전에 다시 받도록 짧게 둔다.
    private static final Duration REDIRECT_CACHE = Duration.ofMinutes(1);

    private final PostService postService;

    // <img src>는 Authorization 헤더를 보낼 수 없으므로 인증 없이 열어 둔다. 주소(UUID)를 아는 사람만 볼 수 있다.
    @GetMapping("/{publicId}")
    public ResponseEntity<Void> redirectToView(@PathVariable UUID publicId) {
        PresignedUrl link = postService.findImageViewUrl(publicId);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, link.url())
                .cacheControl(CacheControl.maxAge(REDIRECT_CACHE).cachePrivate())
                .build();
    }
}
