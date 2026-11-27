package com.ssangtteongnip.domain.post;

import com.ssangtteongnip.domain.image.ImageFormat;
import com.ssangtteongnip.domain.profile.FileStorage;
import com.ssangtteongnip.domain.profile.PresignedUrl;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// 게시글에 첨부한 이미지. 만들기는 Post를 거쳐서만 할 수 있다(개수 한도가 Post에 있다).
// 보여줄 때마다 서버가 짧게 유효한 presigned URL로 안내하므로 버킷은 비공개로 두고, 공개 경로(publicId)는 만료되지 않는다.
@Entity
@Table(name = "post_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImage {

    public static final int MAX_SIZE_BYTES = 5 * 1024 * 1024;
    private static final String PUBLIC_PATH_PREFIX = "/api/post-images/";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    // 검증을 통과한 뒤에만 저장소에 올린다. 검증이나 업로드가 실패하면 객체가 만들어지지 않는다.
    static PostImage upload(Post post, byte[] content, FileStorage storage) {
        ImageFormat format = validate(content);
        PostImage image = new PostImage();
        image.post = post;
        image.publicId = UUID.randomUUID();
        image.objectKey = "post-images/" + post.getUserId() + "/" + image.publicId + "." + format.extension();
        image.contentType = format.contentType();
        image.sizeBytes = content.length;
        storage.put(image.objectKey, content, image.contentType);
        return image;
    }

    public void deleteObject(FileStorage storage) {
        storage.delete(objectKey);
    }

    public PresignedUrl createViewUrl(FileStorage storage) {
        return storage.createViewUrl(objectKey);
    }

    // 응답에 담는 주소. 도메인이 바뀌어도 깨지지 않도록 경로만 둔다.
    public String createPublicPath() {
        return PUBLIC_PATH_PREFIX + publicId;
    }

    private static ImageFormat validate(byte[] content) {
        if (content == null || content.length == 0) {
            throw new BusinessException(ErrorCode.EMPTY_FILE);
        }
        if (content.length > MAX_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
        return ImageFormat.detect(content)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_IMAGE_TYPE));
    }

    @PrePersist
    private void recordCreation() {
        this.createdAt = LocalDateTime.now();
    }
}
