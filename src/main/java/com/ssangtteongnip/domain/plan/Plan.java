package com.ssangtteongnip.domain.plan;

import com.ssangtteongnip.domain.profile.FileStorage;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// 하루의 계획 하나. 날짜마다 여러 개를 쓸 수 있다. 오늘 또는 내일 날짜로만 만들고, 끝내면 결과물을 남긴다.
// 날짜가 지났는데 끝내지 못한 계획은 FAILED가 된다(실패 처리는 failIfExpired).
@Entity
@Table(name = "plans",
        indexes = @Index(name = "idx_plans_user_date", columnList = "user_id, plan_date"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Plan {

    public static final int MAX_EVIDENCES = 5;
    private static final int MIN_ACTUAL_MINUTES = 0;
    private static final int MAX_MEMO_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate planDate;

    @Embedded
    private PlanDetail detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanStatus status;

    private Integer actualMinutes;

    @Enumerated(EnumType.STRING)
    private MissReason missReason;

    @Column(length = MAX_MEMO_LENGTH)
    private String memo;

    private LocalDateTime completedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<PlanEvidence> evidences = new ArrayList<>();

    public static Plan create(Long userId, LocalDate planDate, PlanDetail detail, LocalDate today) {
        if (planDate == null || (!planDate.equals(today) && !planDate.equals(today.plusDays(1)))) {
            throw new BusinessException(ErrorCode.INVALID_PLAN_DATE);
        }
        Plan plan = new Plan();
        plan.userId = userId;
        plan.planDate = planDate;
        plan.detail = detail;
        plan.status = PlanStatus.PLANNED;
        return plan;
    }

    public void revise(PlanDetail detail, LocalDate today) {
        validateModifiable(today);
        this.detail = detail;
    }

    public void validateDeletable(LocalDate today) {
        validateModifiable(today);
    }

    // 계획한 날 안에서만, 결과물을 1개 이상 등록한 뒤에 마칠 수 있다.
    public void complete(int actualMinutes, String memo, LocalDateTime now) {
        if (status != PlanStatus.PLANNED || !planDate.equals(now.toLocalDate())) {
            throw new BusinessException(ErrorCode.PLAN_NOT_COMPLETABLE);
        }
        if (evidences.isEmpty()) {
            throw new BusinessException(ErrorCode.PLAN_EVIDENCE_REQUIRED);
        }
        validateActualMinutes(actualMinutes);
        this.actualMinutes = actualMinutes;
        this.memo = normalizeMemo(memo);
        this.completedAt = now;
        this.status = PlanStatus.DONE;
    }

    // 끝난 뒤의 기록 수정. 값이 null이면 바꾸지 않는다. DONE은 메모만, FAILED는 시간·사유·메모를 고칠 수 있다.
    public void reflect(Integer actualMinutes, MissReason missReason, String memo) {
        if (status == PlanStatus.PLANNED
                || (status == PlanStatus.DONE && (actualMinutes != null || missReason != null))) {
            throw new BusinessException(ErrorCode.PLAN_REFLECTION_NOT_ALLOWED);
        }
        if (actualMinutes != null) {
            validateActualMinutes(actualMinutes);
        }
        String newMemo = memo == null ? this.memo : normalizeMemo(memo);
        if (actualMinutes != null) {
            this.actualMinutes = actualMinutes;
        }
        if (missReason != null) {
            this.missReason = missReason;
        }
        this.memo = newMemo;
    }

    // 날짜가 지났는데 아직 진행 전이면 실패로 바꾸고 true를 돌려준다. 이미 처리됐거나 해당이 없으면 false.
    public boolean failIfExpired(LocalDate today) {
        if (status != PlanStatus.PLANNED || !planDate.isBefore(today)) {
            return false;
        }
        this.status = PlanStatus.FAILED;
        return true;
    }

    public PlanEvidence addLink(String url, LocalDate today) {
        validateEvidenceAddable(today);
        PlanEvidence evidence = PlanEvidence.createLink(this, url);
        evidences.add(evidence);
        return evidence;
    }

    public PlanEvidence addFile(String fileName, byte[] content, FileStorage storage, LocalDate today) {
        validateEvidenceAddable(today);
        PlanEvidence evidence = PlanEvidence.createFile(this, fileName, content, storage);
        evidences.add(evidence);
        return evidence;
    }

    // 완료한 계획은 결과물이 1개 이상 남아야 하므로 마지막 하나는 지울 수 없다. 진행 전 계획은 모두 지울 수 있다.
    public PlanEvidence removeEvidence(Long evidenceId, LocalDate today) {
        if (status == PlanStatus.DONE) {
            validateOtherEvidenceRemains();
        } else {
            validateModifiable(today);
        }
        PlanEvidence evidence = findEvidence(evidenceId);
        evidences.remove(evidence);
        return evidence;
    }

    public PlanEvidence findEvidence(Long evidenceId) {
        return evidences.stream()
                .filter(evidence -> evidenceId.equals(evidence.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_EVIDENCE_NOT_FOUND));
    }

    public List<PlanEvidence> getEvidences() {
        return Collections.unmodifiableList(evidences);
    }

    private void validateOtherEvidenceRemains() {
        if (evidences.size() <= 1) {
            throw new BusinessException(ErrorCode.PLAN_EVIDENCE_REQUIRED);
        }
    }

    private void validateModifiable(LocalDate today) {
        if (status != PlanStatus.PLANNED || planDate.isBefore(today)) {
            throw new BusinessException(ErrorCode.PLAN_NOT_MODIFIABLE);
        }
    }

    private void validateEvidenceAddable(LocalDate today) {
        boolean plannedAndNotExpired = status == PlanStatus.PLANNED && !planDate.isBefore(today);
        if (status != PlanStatus.DONE && !plannedAndNotExpired) {
            throw new BusinessException(ErrorCode.PLAN_EVIDENCE_NOT_ALLOWED);
        }
        if (evidences.size() >= MAX_EVIDENCES) {
            throw new BusinessException(ErrorCode.PLAN_EVIDENCE_LIMIT_EXCEEDED);
        }
    }

    private static void validateActualMinutes(int actualMinutes) {
        if (actualMinutes < MIN_ACTUAL_MINUTES || actualMinutes > PlanDetail.MAX_MINUTES) {
            throw new BusinessException(ErrorCode.INVALID_ACTUAL_MINUTES);
        }
    }

    // 비어 있는 메모는 null로 저장한다.
    private static String normalizeMemo(String memo) {
        if (memo == null || memo.isBlank()) {
            return null;
        }
        if (memo.length() > MAX_MEMO_LENGTH) {
            throw new BusinessException(ErrorCode.PLAN_TEXT_TOO_LONG);
        }
        return memo;
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
