package com.ssangtteongnip.domain.post;

import com.ssangtteongnip.domain.profile.FileStorage;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// 취준생 게시판의 글. 이미지는 최대 MAX_IMAGES장이고, 개수·작성자·길이 규칙은 모두 여기서 지킨다.
@Slf4j
@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    public static final int MAX_TITLE_LENGTH = 100;
    public static final int MAX_CONTENT_LENGTH = 5000;
    public static final int MAX_IMAGES = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = MAX_TITLE_LENGTH)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<PostImage> images = new ArrayList<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // 글과 이미지를 검증한 뒤에만 이미지를 저장소에 올린다.
    public static Post create(Long userId, String title, String content, List<byte[]> newImages, FileStorage storage) {
        validateText(title, content);
        validateImageCount(newImages.size());
        Post post = new Post();
        post.userId = userId;
        post.title = title;
        post.content = content;
        post.attach(newImages, storage);
        return post;
    }

    // 남길 이미지와 새 이미지를 합쳐 한도 안인지 먼저 확인하고, 새 이미지를 모두 올린 뒤에만 글을 바꾼다.
    // 중간에 실패하면 글은 그대로이고 이미 올린 이미지는 지운다. 글에서 빠진 이미지를 돌려주므로 호출한 쪽이 저장소 객체를 지운다.
    public List<PostImage> rewrite(String title, String content, Set<UUID> keepImageIds,
                                   List<byte[]> newImages, FileStorage storage) {
        validateText(title, content);
        validateKeepable(keepImageIds);
        validateImageCount(keepImageIds.size() + newImages.size());
        List<PostImage> removed = images.stream()
                .filter(image -> !keepImageIds.contains(image.getPublicId()))
                .toList();
        attach(newImages, storage);
        this.title = title;
        this.content = content;
        images.removeAll(removed);
        return removed;
    }

    public boolean isWrittenBy(Long userId) {
        return this.userId.equals(userId);
    }

    public void validateWrittenBy(Long userId) {
        if (!isWrittenBy(userId)) {
            throw new BusinessException(ErrorCode.POST_NOT_WRITER);
        }
    }

    public int countImages() {
        return images.size();
    }

    public List<PostImage> getImages() {
        return Collections.unmodifiableList(images);
    }

    // 저장소 객체를 모두 지운다. 이미 끝난 요청을 깨뜨리지 않도록 실패는 로그만 남긴다.
    public void deleteImageObjects(FileStorage storage) {
        deleteObjects(images, storage);
    }

    private void attach(List<byte[]> contents, FileStorage storage) {
        List<PostImage> uploaded = new ArrayList<>();
        try {
            for (byte[] content : contents) {
                uploaded.add(PostImage.upload(this, content, storage));
            }
        } catch (RuntimeException e) {
            deleteObjects(uploaded, storage);
            throw e;
        }
        images.addAll(uploaded);
    }

    private static void deleteObjects(List<PostImage> targets, FileStorage storage) {
        for (PostImage image : targets) {
            try {
                image.deleteObject(storage);
            } catch (RuntimeException e) {
                log.warn("게시글 이미지 객체 삭제 실패: {}", image.getObjectKey(), e);
            }
        }
    }

    private void validateKeepable(Set<UUID> keepImageIds) {
        Set<UUID> ownIds = images.stream().map(PostImage::getPublicId).collect(Collectors.toSet());
        if (!ownIds.containsAll(keepImageIds)) {
            throw new BusinessException(ErrorCode.POST_IMAGE_NOT_FOUND);
        }
    }

    private static void validateText(String title, String content) {
        if (title == null || title.isBlank() || title.length() > MAX_TITLE_LENGTH) {
            throw new BusinessException(ErrorCode.POST_TITLE_REQUIRED);
        }
        if (content == null || content.isBlank() || content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.POST_CONTENT_REQUIRED);
        }
    }

    private static void validateImageCount(int count) {
        if (count > MAX_IMAGES) {
            throw new BusinessException(ErrorCode.POST_IMAGE_LIMIT_EXCEEDED);
        }
    }

    @PrePersist
    private void recordCreation() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void recordModification() {
        this.updatedAt = LocalDateTime.now();
    }
}
