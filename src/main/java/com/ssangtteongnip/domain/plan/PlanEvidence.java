package com.ssangtteongnip.domain.plan;

import com.ssangtteongnip.domain.image.ImageFormat;
import com.ssangtteongnip.domain.profile.FileStorage;
import com.ssangtteongnip.domain.profile.PresignedUrl;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

// 계획을 마쳤다는 증거로 남기는 결과물. 링크(URL) 또는 업로드한 파일(FILE) 중 하나다.
// 만들기는 Plan을 거쳐서만 할 수 있다(개수 한도와 상태 규칙이 Plan에 있다).
@Entity
@Table(name = "plan_evidences")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanEvidence {

    public static final int MAX_SIZE_BYTES = 5 * 1024 * 1024;
    static final int MAX_URL_LENGTH = 500;
    private static final String DEFAULT_FILE_NAME = "file";
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String PDF_EXTENSION = "pdf";
    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvidenceType type;

    @Column(length = MAX_URL_LENGTH)
    private String url;

    private String fileName;

    @Column(name = "object_key")
    private String objectKey;

    private String contentType;

    private Long sizeBytes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    static PlanEvidence createLink(Plan plan, String url) {
        validateUrl(url);
        PlanEvidence evidence = new PlanEvidence();
        evidence.plan = plan;
        evidence.type = EvidenceType.URL;
        evidence.url = url;
        return evidence;
    }

    // 검증을 통과한 뒤에만 저장소에 올린다. 검증이나 업로드가 실패하면 결과물은 만들어지지 않는다.
    static PlanEvidence createFile(Plan plan, String fileName, byte[] content, FileStorage storage) {
        validateSize(content);
        FileKind kind = detectKind(content);
        PlanEvidence evidence = new PlanEvidence();
        evidence.plan = plan;
        evidence.type = EvidenceType.FILE;
        evidence.fileName = sanitize(fileName);
        evidence.contentType = kind.contentType();
        evidence.sizeBytes = (long) content.length;
        evidence.objectKey = "plan-evidences/" + plan.getUserId() + "/" + UUID.randomUUID() + "." + kind.extension();
        storage.put(evidence.objectKey, content, evidence.contentType);
        return evidence;
    }

    public boolean isFile() {
        return type == EvidenceType.FILE;
    }

    public PresignedUrl createDownloadUrl(FileStorage storage) {
        if (!isFile()) {
            throw new BusinessException(ErrorCode.PLAN_EVIDENCE_NOT_FILE);
        }
        return storage.createDownloadUrl(objectKey, fileName);
    }

    // 링크 결과물은 저장소에 올린 것이 없으므로 지울 것도 없다.
    public void deleteFile(FileStorage storage) {
        if (isFile()) {
            storage.delete(objectKey);
        }
    }

    private static void validateUrl(String url) {
        if (url == null || url.isBlank() || url.length() > MAX_URL_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_EVIDENCE_URL);
        }
        try {
            URI uri = new URI(url);
            boolean web = "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
            if (!web || uri.getHost() == null) {
                throw new BusinessException(ErrorCode.INVALID_EVIDENCE_URL);
            }
        } catch (URISyntaxException e) {
            throw new BusinessException(ErrorCode.INVALID_EVIDENCE_URL);
        }
    }

    private static void validateSize(byte[] content) {
        if (content == null || content.length == 0) {
            throw new BusinessException(ErrorCode.EMPTY_FILE);
        }
        if (content.length > MAX_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
    }

    // 확장자나 Content-Type은 클라이언트가 속일 수 있으므로 파일 앞부분(매직 바이트)으로 판별한다.
    private static FileKind detectKind(byte[] content) {
        return ImageFormat.detect(content)
                .map(format -> new FileKind(format.contentType(), format.extension()))
                .or(() -> detectPdf(content))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_EVIDENCE_FILE_TYPE));
    }

    private static Optional<FileKind> detectPdf(byte[] content) {
        boolean pdf = content.length >= PDF_MAGIC.length
                && Arrays.equals(content, 0, PDF_MAGIC.length, PDF_MAGIC, 0, PDF_MAGIC.length);
        return pdf ? Optional.of(new FileKind(PDF_CONTENT_TYPE, PDF_EXTENSION)) : Optional.empty();
    }

    private static String sanitize(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return DEFAULT_FILE_NAME;
        }
        String name = fileName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        return name.isEmpty() ? DEFAULT_FILE_NAME : name;
    }

    private record FileKind(String contentType, String extension) {}

    @PrePersist
    private void recordCreation() {
        this.createdAt = LocalDateTime.now();
    }
}
