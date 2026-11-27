package com.ssangtteongnip.web.post;

import com.ssangtteongnip.application.service.PostService;
import com.ssangtteongnip.application.service.dto.PostPageResponse;
import com.ssangtteongnip.application.service.dto.PostResponse;
import com.ssangtteongnip.application.service.request.PostRequest;
import com.ssangtteongnip.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // 글에 이미지가 붙어 multipart로 받는다. 파일은 application이 web을 참조하지 않도록 바이트로 풀어 넘긴다.
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostResponse> create(@AuthenticationPrincipal Long userId,
                                            @RequestParam String title,
                                            @RequestParam String content,
                                            @RequestParam(required = false) List<MultipartFile> images) throws IOException {
        PostRequest request = new PostRequest(title, content, null, readAll(images));
        return ApiResponse.ok(postService.create(userId, request));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PostResponse> rewrite(@AuthenticationPrincipal Long userId,
                                             @PathVariable Long id,
                                             @RequestParam String title,
                                             @RequestParam String content,
                                             @RequestParam(required = false) Set<UUID> keepImageIds,
                                             @RequestParam(required = false) List<MultipartFile> images) throws IOException {
        PostRequest request = new PostRequest(title, content, keepImageIds, readAll(images));
        return ApiResponse.ok(postService.rewrite(userId, id, request));
    }

    @GetMapping
    public ApiResponse<PostPageResponse> findPage(@RequestParam(defaultValue = "0") int page) {
        return ApiResponse.ok(postService.findPage(page));
    }

    @GetMapping("/{id}")
    public ApiResponse<PostResponse> find(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return ApiResponse.ok(postService.find(userId, id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        postService.delete(userId, id);
        return ApiResponse.ok();
    }

    private static List<byte[]> readAll(List<MultipartFile> files) throws IOException {
        List<byte[]> contents = new ArrayList<>();
        if (files == null) {
            return contents;
        }
        for (MultipartFile file : files) {
            contents.add(file.getBytes());
        }
        return contents;
    }
}
