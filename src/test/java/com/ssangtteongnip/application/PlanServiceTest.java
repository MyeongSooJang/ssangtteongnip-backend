package com.ssangtteongnip.application;

import com.ssangtteongnip.application.service.PlanService;
import com.ssangtteongnip.application.service.dto.FileDownload;
import com.ssangtteongnip.application.service.dto.PlanEvidenceResponse;
import com.ssangtteongnip.application.service.dto.PlanResponse;
import com.ssangtteongnip.application.service.request.PlanCompleteRequest;
import com.ssangtteongnip.application.service.request.PlanCreateRequest;
import com.ssangtteongnip.application.service.request.PlanDetailRequest;
import com.ssangtteongnip.application.service.request.PlanReflectionRequest;
import com.ssangtteongnip.domain.FakeFileStorage;
import com.ssangtteongnip.domain.plan.MissReason;
import com.ssangtteongnip.domain.plan.Plan;
import com.ssangtteongnip.domain.plan.PlanCategory;
import com.ssangtteongnip.domain.plan.PlanDetail;
import com.ssangtteongnip.domain.plan.PlanEvidence;
import com.ssangtteongnip.domain.plan.PlanRepository;
import com.ssangtteongnip.domain.plan.PlanStatus;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long PLAN_ID = 10L;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 25);
    private static final String LINK = "https://github.com/ssangtteongnip/job-tracker";

    @Mock PlanRepository planRepository;
    FakeFileStorage storage = new FakeFileStorage();
    PlanService planService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-25T03:00:00Z"), SEOUL);
        planService = new PlanService(planRepository, storage, clock);
    }

    private static PlanDetail detail() {
        return new PlanDetail("이력서 수정", PlanCategory.APPLY, "PDF로 저장한다", 60, null, null, null);
    }

    private static Plan planOn(LocalDate date) {
        return Plan.create(USER_ID, date, detail(), date);
    }

    private void givenOwnedPlan(Plan plan) {
        given(planRepository.findByIdAndUserId(PLAN_ID, USER_ID)).willReturn(Optional.of(plan));
    }

    private void givenOwnedPlanForUpdate(Plan plan) {
        given(planRepository.findOwnedForUpdate(PLAN_ID, USER_ID)).willReturn(Optional.of(plan));
    }

    private static byte[] pdf() {
        return "%PDF-1.7\nbody".getBytes(StandardCharsets.US_ASCII);
    }

    @Test
    void 계획을_만들면_진행_전_상태로_저장된다() {
        given(planRepository.save(any(Plan.class))).willAnswer(invocation -> invocation.getArgument(0));

        PlanResponse response = planService.create(USER_ID, new PlanCreateRequest(
                TODAY.plusDays(1), "이력서 수정", PlanCategory.APPLY, "PDF로 저장한다", 60, null, null, null));

        assertThat(response.planDate()).isEqualTo(TODAY.plusDays(1));
        assertThat(response.status()).isEqualTo(PlanStatus.PLANNED);
        assertThat(response.evidences()).isEmpty();
    }

    @Test
    void 어제_날짜로는_계획을_만들_수_없다() {
        assertErrorCode(() -> planService.create(USER_ID, new PlanCreateRequest(
                        TODAY.minusDays(1), "이력서 수정", PlanCategory.APPLY, "PDF로 저장한다", 60, null, null, null)),
                ErrorCode.INVALID_PLAN_DATE);
        verify(planRepository, never()).save(any());
    }

    @Test
    void 계획을_고치면_새_내용으로_바뀐다() {
        Plan plan = planOn(TODAY);
        givenOwnedPlan(plan);

        PlanResponse response = planService.revise(USER_ID, PLAN_ID,
                new PlanDetailRequest("포트폴리오 정리", PlanCategory.OUTPUT, "README 완성", 90, null, null, null));

        assertThat(response.content()).isEqualTo("포트폴리오 정리");
        assertThat(response.estimatedMinutes()).isEqualTo(90);
    }

    @Test
    void 다른_사용자의_계획은_고칠_수_없다() {
        given(planRepository.findByIdAndUserId(PLAN_ID, OTHER_USER_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> planService.revise(OTHER_USER_ID, PLAN_ID,
                        new PlanDetailRequest("내용", PlanCategory.APPLY, "기준", 30, null, null, null)),
                ErrorCode.PLAN_NOT_FOUND);
    }

    @Test
    void 결과물이_있는_오늘_계획을_완료하면_완료_상태가_된다() {
        Plan plan = planOn(TODAY);
        plan.addLink(LINK, TODAY);
        givenOwnedPlanForUpdate(plan);

        PlanResponse response = planService.complete(USER_ID, PLAN_ID, new PlanCompleteRequest(45, "집중"));

        assertThat(response.status()).isEqualTo(PlanStatus.DONE);
        assertThat(response.actualMinutes()).isEqualTo(45);
        assertThat(response.completedAt()).isEqualTo(TODAY.atTime(12, 0));
    }

    @Test
    void 결과물이_없으면_완료할_수_없다() {
        givenOwnedPlanForUpdate(planOn(TODAY));

        assertErrorCode(() -> planService.complete(USER_ID, PLAN_ID, new PlanCompleteRequest(45, null)),
                ErrorCode.PLAN_EVIDENCE_REQUIRED);
    }

    @Test
    void 실패한_계획의_사유와_시간을_기록한다() {
        Plan plan = planOn(TODAY.minusDays(1));
        plan.failIfExpired(TODAY);
        givenOwnedPlan(plan);

        PlanResponse response = planService.reflect(USER_ID, PLAN_ID,
                new PlanReflectionRequest(20, MissReason.TOO_BIG, "목표가 컸다"));

        assertThat(response.missReason()).isEqualTo(MissReason.TOO_BIG);
        assertThat(response.actualMinutes()).isEqualTo(20);
        assertThat(response.status()).isEqualTo(PlanStatus.FAILED);
    }

    @Test
    void 진행_전_계획을_삭제하면_결과물_파일도_저장소에서_지운다() {
        Plan plan = planOn(TODAY);
        PlanEvidence file = plan.addFile("a.pdf", pdf(), storage, TODAY);
        givenOwnedPlan(plan);

        planService.delete(USER_ID, PLAN_ID);

        verify(planRepository).delete(plan);
        assertThat(storage.contains(file.getObjectKey())).isFalse();
    }

    @Test
    void 완료한_계획은_삭제할_수_없고_저장소_파일도_남는다() {
        Plan plan = planOn(TODAY);
        PlanEvidence file = plan.addFile("a.pdf", pdf(), storage, TODAY);
        plan.complete(30, null, TODAY.atTime(12, 0));
        givenOwnedPlan(plan);

        assertErrorCode(() -> planService.delete(USER_ID, PLAN_ID), ErrorCode.PLAN_NOT_MODIFIABLE);

        verify(planRepository, never()).delete(any());
        assertThat(storage.contains(file.getObjectKey())).isTrue();
    }

    @Test
    void 기간으로_계획_목록을_조회한다() {
        given(planRepository.findInPeriod(USER_ID, TODAY, TODAY.plusDays(6))).willReturn(List.of(planOn(TODAY)));

        List<PlanResponse> responses = planService.findRange(USER_ID, TODAY, TODAY.plusDays(6));

        assertThat(responses).hasSize(1);
    }

    @Test
    void 조회_기간이_62일을_넘으면_조회할_수_없다() {
        assertErrorCode(() -> planService.findRange(USER_ID, TODAY, TODAY.plusDays(62)),
                ErrorCode.INVALID_STUDY_RANGE);
    }

    @Test
    void 링크_결과물을_추가하면_저장한_결과물을_돌려준다() {
        Plan plan = planOn(TODAY);
        givenOwnedPlanForUpdate(plan);

        PlanEvidenceResponse response = planService.addLink(USER_ID, PLAN_ID, LINK);

        assertThat(response.url()).isEqualTo(LINK);
        verify(planRepository).flush();
    }

    @Test
    void 결과물_개수가_이미_한도에_닿은_계획에는_추가할_수_없다() {
        Plan plan = planOn(TODAY);
        for (int i = 0; i < Plan.MAX_EVIDENCES; i++) {
            plan.addLink(LINK + i, TODAY);
        }
        givenOwnedPlanForUpdate(plan);

        assertErrorCode(() -> planService.addLink(USER_ID, PLAN_ID, LINK), ErrorCode.PLAN_EVIDENCE_LIMIT_EXCEEDED);
        verify(planRepository, never()).flush();
    }

    @Test
    void 파일_결과물을_추가하면_저장소에_올라간다() {
        givenOwnedPlanForUpdate(planOn(TODAY));

        PlanEvidenceResponse response = planService.addFile(USER_ID, PLAN_ID, "a.pdf", pdf());

        assertThat(response.fileName()).isEqualTo("a.pdf");
        assertThat(storage.size()).isEqualTo(1);
    }

    @Test
    void 파일을_올린_뒤_DB_반영에_실패하면_올린_파일을_지운다() {
        givenOwnedPlanForUpdate(planOn(TODAY));
        willThrow(new IllegalStateException("flush 실패")).given(planRepository).flush();

        assertThatThrownBy(() -> planService.addFile(USER_ID, PLAN_ID, "a.pdf", pdf()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(storage.size()).isZero();
    }

    @Test
    void 결과물을_삭제하면_계획에서_빠지고_저장소_파일도_지운다() {
        Plan plan = planOn(TODAY);
        PlanEvidence file = plan.addFile("a.pdf", pdf(), storage, TODAY);
        ReflectionTestUtils.setField(file, "id", 5L);
        givenOwnedPlanForUpdate(plan);

        planService.removeEvidence(USER_ID, PLAN_ID, 5L);

        assertThat(plan.getEvidences()).isEmpty();
        assertThat(storage.contains(file.getObjectKey())).isFalse();
    }

    @Test
    void 파일_결과물의_내려받기_주소를_돌려준다() {
        Plan plan = planOn(TODAY);
        PlanEvidence file = plan.addFile("a.pdf", pdf(), storage, TODAY);
        ReflectionTestUtils.setField(file, "id", 5L);
        givenOwnedPlan(plan);

        FileDownload download = planService.findEvidenceDownload(USER_ID, PLAN_ID, 5L);

        assertThat(download.fileName()).isEqualTo("a.pdf");
        assertThat(download.url()).contains(file.getObjectKey());
    }

    @Test
    void 여러_날_밀린_진행_전_계획이_한_번에_실패로_바뀐다() {
        Plan twoDaysAgo = planOn(TODAY.minusDays(2));
        Plan yesterday = planOn(TODAY.minusDays(1));
        given(planRepository.findPlannedBefore(TODAY)).willReturn(List.of(twoDaysAgo, yesterday));

        int failed = planService.failExpiredPlans();

        assertThat(failed).isEqualTo(2);
        assertThat(twoDaysAgo.getStatus()).isEqualTo(PlanStatus.FAILED);
        assertThat(yesterday.getStatus()).isEqualTo(PlanStatus.FAILED);
    }

    @Test
    void 자동_실패를_두_번_실행해도_결과가_같다() {
        Plan yesterday = planOn(TODAY.minusDays(1));
        given(planRepository.findPlannedBefore(TODAY)).willReturn(List.of(yesterday));

        int first = planService.failExpiredPlans();
        int second = planService.failExpiredPlans();

        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
        assertThat(yesterday.getStatus()).isEqualTo(PlanStatus.FAILED);
    }

    @Test
    void 오늘_계획은_자동_실패_대상이_아니다() {
        Plan todayPlan = planOn(TODAY);
        given(planRepository.findPlannedBefore(TODAY)).willReturn(List.of(todayPlan));

        int failed = planService.failExpiredPlans();

        assertThat(failed).isZero();
        assertThat(todayPlan.getStatus()).isEqualTo(PlanStatus.PLANNED);
    }
}
