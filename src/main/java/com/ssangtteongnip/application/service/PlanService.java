package com.ssangtteongnip.application.service;

import com.ssangtteongnip.application.service.dto.FileDownload;
import com.ssangtteongnip.application.service.dto.PlanEvidenceResponse;
import com.ssangtteongnip.application.service.dto.PlanResponse;
import com.ssangtteongnip.application.service.request.PlanCompleteRequest;
import com.ssangtteongnip.application.service.request.PlanCreateRequest;
import com.ssangtteongnip.application.service.request.PlanDetailRequest;
import com.ssangtteongnip.application.service.request.PlanReflectionRequest;
import com.ssangtteongnip.domain.plan.Plan;
import com.ssangtteongnip.domain.plan.PlanEvidence;
import com.ssangtteongnip.domain.plan.PlanRepository;
import com.ssangtteongnip.domain.profile.FileStorage;
import com.ssangtteongnip.domain.profile.PresignedUrl;
import com.ssangtteongnip.domain.studylog.StudyPeriod;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import com.ssangtteongnip.global.transaction.AfterCommit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanService {

    private static final int MAX_LIST_DAYS = 62;

    private final PlanRepository planRepository;
    private final FileStorage fileStorage;
    private final Clock clock;

    @Transactional
    public PlanResponse create(Long userId, PlanCreateRequest request) {
        Plan created = Plan.create(userId, request.planDate(), request.createDetail(), findToday());
        return PlanResponse.convert(planRepository.save(created));
    }

    @Transactional
    public PlanResponse revise(Long userId, Long id, PlanDetailRequest request) {
        Plan plan = findOrThrow(userId, id);
        plan.revise(request.createDetail(), findToday());
        return PlanResponse.convert(plan);
    }

    @Transactional
    public PlanResponse complete(Long userId, Long id, PlanCompleteRequest request) {
        // 결과물 삭제와 동시에 일어나도 결과물 없이 완료되지 않도록 계획 행을 잠그고 확인한다.
        Plan plan = findForUpdateOrThrow(userId, id);
        plan.complete(request.actualMinutes(), request.memo(), LocalDateTime.now(clock));
        return PlanResponse.convert(plan);
    }

    @Transactional
    public PlanResponse reflect(Long userId, Long id, PlanReflectionRequest request) {
        Plan plan = findOrThrow(userId, id);
        plan.reflect(request.actualMinutes(), request.missReason(), request.memo());
        return PlanResponse.convert(plan);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Plan plan = findOrThrow(userId, id);
        plan.validateDeletable(findToday());
        planRepository.delete(plan);
        plan.getEvidences().forEach(this::deleteFileAfterCommit);
    }

    public List<PlanResponse> findRange(Long userId, LocalDate from, LocalDate to) {
        StudyPeriod period = StudyPeriod.createWithin(from, to, MAX_LIST_DAYS);
        return planRepository.findInPeriod(userId, period.from(), period.to())
                .stream().map(PlanResponse::convert).toList();
    }

    // 개수 한도를 동시 요청이 함께 넘지 못하도록 계획 행을 잠근 뒤 추가한다.
    @Transactional
    public PlanEvidenceResponse addLink(Long userId, Long id, String url) {
        Plan plan = findForUpdateOrThrow(userId, id);
        PlanEvidence evidence = plan.addLink(url, findToday());
        planRepository.flush();
        return PlanEvidenceResponse.convert(evidence);
    }

    @Transactional
    public PlanEvidenceResponse addFile(Long userId, Long id, String fileName, byte[] content) {
        Plan plan = findForUpdateOrThrow(userId, id);
        PlanEvidence evidence = plan.addFile(fileName, content, fileStorage, findToday());
        flushOrDeleteUploaded(evidence);
        return PlanEvidenceResponse.convert(evidence);
    }

    @Transactional
    public void removeEvidence(Long userId, Long id, Long evidenceId) {
        Plan plan = findForUpdateOrThrow(userId, id);
        PlanEvidence removed = plan.removeEvidence(evidenceId, findToday());
        deleteFileAfterCommit(removed);
    }

    public FileDownload findEvidenceDownload(Long userId, Long id, Long evidenceId) {
        PlanEvidence evidence = findOrThrow(userId, id).findEvidence(evidenceId);
        PresignedUrl link = evidence.createDownloadUrl(fileStorage);
        return new FileDownload(link.url(), evidence.getFileName(), link.expiresAt());
    }

    // 날짜가 지난 진행 전 계획을 모두 실패로 바꾸고, 바꾼 건 수를 돌려준다. 몇 번을 실행해도 결과가 같다.
    @Transactional
    public int failExpiredPlans() {
        LocalDate today = findToday();
        int failed = 0;
        for (Plan plan : planRepository.findPlannedBefore(today)) {
            if (plan.failIfExpired(today)) {
                failed++;
            }
        }
        return failed;
    }

    // 저장소에는 이미 올라갔으므로 DB 반영이 실패하면 올린 파일이 고아로 남지 않게 지우고 예외를 다시 던진다.
    private void flushOrDeleteUploaded(PlanEvidence uploaded) {
        try {
            planRepository.flush();
        } catch (RuntimeException e) {
            uploaded.deleteFile(fileStorage);
            throw e;
        }
    }

    private void deleteFileAfterCommit(PlanEvidence evidence) {
        AfterCommit.runQuietly("결과물 파일 삭제", () -> evidence.deleteFile(fileStorage));
    }

    private Plan findOrThrow(Long userId, Long id) {
        return planRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));
    }

    private Plan findForUpdateOrThrow(Long userId, Long id) {
        return planRepository.findOwnedForUpdate(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));
    }

    private LocalDate findToday() {
        return LocalDate.now(clock);
    }
}
