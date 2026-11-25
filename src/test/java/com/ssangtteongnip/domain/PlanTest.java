package com.ssangtteongnip.domain;

import com.ssangtteongnip.domain.plan.EvidenceType;
import com.ssangtteongnip.domain.plan.MissReason;
import com.ssangtteongnip.domain.plan.Plan;
import com.ssangtteongnip.domain.plan.PlanCategory;
import com.ssangtteongnip.domain.plan.PlanDetail;
import com.ssangtteongnip.domain.plan.PlanEvidence;
import com.ssangtteongnip.domain.plan.PlanStatus;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;

class PlanTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 25);
    private static final LocalDate TOMORROW = TODAY.plusDays(1);
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);
    private static final LocalDateTime NOON = TODAY.atTime(12, 0);
    private static final String LINK = "https://github.com/ssangtteongnip/job-tracker";

    private final FakeFileStorage storage = new FakeFileStorage();

    private static PlanDetail detail() {
        return new PlanDetail("이력서 프로젝트 항목 3줄 수정", PlanCategory.APPLY, "수정본을 PDF로 저장한다", 60,
                null, null, null);
    }

    private static Plan planOn(LocalDate date) {
        return Plan.create(USER_ID, date, detail(), date);
    }

    private static Plan planWithLink(LocalDate date) {
        Plan plan = planOn(date);
        PlanEvidence evidence = plan.addLink(LINK, date);
        ReflectionTestUtils.setField(evidence, "id", 100L);
        return plan;
    }

    private static Plan doneToday() {
        Plan plan = planWithLink(TODAY);
        plan.complete(50, null, NOON);
        return plan;
    }

    private static Plan failedPlan() {
        Plan plan = planOn(YESTERDAY);
        plan.failIfExpired(TODAY);
        return plan;
    }

    private static byte[] pdf() {
        return "%PDF-1.7\nbody".getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] png() {
        return new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
    }

    // ---- 생성 ----

    @Test
    void 계획을_새로_만들면_진행_전_상태이다() {
        Plan plan = planOn(TODAY);

        assertThat(plan.getStatus()).isEqualTo(PlanStatus.PLANNED);
        assertThat(plan.getPlanDate()).isEqualTo(TODAY);
        assertThat(plan.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    void 내일_날짜로도_만들_수_있다() {
        Plan plan = Plan.create(USER_ID, TOMORROW, detail(), TODAY);

        assertThat(plan.getPlanDate()).isEqualTo(TOMORROW);
    }

    @Test
    void 같은_날짜에_여러_계획을_만들_수_있다() {
        Plan first = Plan.create(USER_ID, TOMORROW, detail(), TODAY);
        Plan second = Plan.create(USER_ID, TOMORROW, detail(), TODAY);

        assertThat(first.getPlanDate()).isEqualTo(second.getPlanDate());
    }

    @Test
    void 어제_날짜로는_만들_수_없다() {
        assertErrorCode(() -> Plan.create(USER_ID, YESTERDAY, detail(), TODAY), ErrorCode.INVALID_PLAN_DATE);
    }

    @Test
    void 모레_날짜로는_만들_수_없다() {
        assertErrorCode(() -> Plan.create(USER_ID, TODAY.plusDays(2), detail(), TODAY), ErrorCode.INVALID_PLAN_DATE);
    }

    @Test
    void 날짜가_없으면_만들_수_없다() {
        assertErrorCode(() -> Plan.create(USER_ID, null, detail(), TODAY), ErrorCode.INVALID_PLAN_DATE);
    }

    // ---- 계획 내용 ----

    @Test
    void 내용이_50자이면_만들_수_있다() {
        PlanDetail fifty = new PlanDetail("가".repeat(50), PlanCategory.SKILL, "기준", 1, null, null, null);

        assertThat(fifty.getContent()).hasSize(50);
    }

    @Test
    void 내용이_51자이면_만들_수_없다() {
        assertErrorCode(() -> new PlanDetail("가".repeat(51), PlanCategory.SKILL, "기준", 1, null, null, null),
                ErrorCode.PLAN_CONTENT_REQUIRED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void 내용이_비어_있으면_만들_수_없다(String content) {
        assertErrorCode(() -> new PlanDetail(content, PlanCategory.SKILL, "기준", 60, null, null, null),
                ErrorCode.PLAN_CONTENT_REQUIRED);
    }

    @Test
    void 종류가_없으면_만들_수_없다() {
        assertErrorCode(() -> new PlanDetail("내용", null, "기준", 60, null, null, null),
                ErrorCode.PLAN_CATEGORY_REQUIRED);
    }

    @Test
    void 완료_기준이_비어_있으면_만들_수_없다() {
        assertErrorCode(() -> new PlanDetail("내용", PlanCategory.LIFE, " ", 60, null, null, null),
                ErrorCode.PLAN_DONE_CRITERIA_REQUIRED);
    }

    @Test
    void 목표_시간이_1분이면_만들_수_있고_0분이면_만들_수_없다() {
        PlanDetail oneMinute = new PlanDetail("내용", PlanCategory.LIFE, "기준", 1, null, null, null);

        assertThat(oneMinute.getEstimatedMinutes()).isEqualTo(1);
        assertErrorCode(() -> new PlanDetail("내용", PlanCategory.LIFE, "기준", 0, null, null, null),
                ErrorCode.INVALID_ESTIMATED_MINUTES);
    }

    @Test
    void 목표_시간이_하루_길이인_1440분을_넘으면_만들_수_없다() {
        PlanDetail fullDay = new PlanDetail("내용", PlanCategory.LIFE, "기준", 1440, null, null, null);

        assertThat(fullDay.getEstimatedMinutes()).isEqualTo(1440);
        assertErrorCode(() -> new PlanDetail("내용", PlanCategory.LIFE, "기준", 1441, null, null, null),
                ErrorCode.INVALID_ESTIMATED_MINUTES);
    }

    @Test
    void 비어_있는_선택_항목은_null로_저장한다() {
        PlanDetail detail = new PlanDetail("내용", PlanCategory.LIFE, "기준", 30, " ", "", "책상 앞에 앉기");

        assertThat(detail.getTimeSlot()).isNull();
        assertThat(detail.getPlace()).isNull();
        assertThat(detail.getFirstAction()).isEqualTo("책상 앞에 앉기");
    }

    @Test
    void 선택_항목이_100자를_넘으면_만들_수_없다() {
        assertErrorCode(() -> new PlanDetail("내용", PlanCategory.LIFE, "기준", 30, null, "가".repeat(101), null),
                ErrorCode.PLAN_TEXT_TOO_LONG);
    }

    // ---- 수정·삭제 ----

    @Test
    void 진행_전_계획은_내용을_고칠_수_있다() {
        Plan plan = planOn(TOMORROW);
        PlanDetail revised = new PlanDetail("포트폴리오 정리", PlanCategory.OUTPUT, "README 완성", 90, null, null, null);

        plan.revise(revised, TODAY);

        assertThat(plan.getDetail().getContent()).isEqualTo("포트폴리오 정리");
        assertThat(plan.getDetail().getEstimatedMinutes()).isEqualTo(90);
    }

    @Test
    void 완료한_계획은_고칠_수_없다() {
        assertErrorCode(() -> doneToday().revise(detail(), TODAY), ErrorCode.PLAN_NOT_MODIFIABLE);
    }

    @Test
    void 실패한_계획은_고칠_수_없다() {
        assertErrorCode(() -> failedPlan().revise(detail(), TODAY), ErrorCode.PLAN_NOT_MODIFIABLE);
    }

    @Test
    void 날짜가_지났지만_아직_실패_처리되지_않은_계획도_고칠_수_없다() {
        assertErrorCode(() -> planOn(YESTERDAY).revise(detail(), TODAY), ErrorCode.PLAN_NOT_MODIFIABLE);
    }

    @Test
    void 진행_전_계획은_삭제할_수_있다() {
        planOn(TODAY).validateDeletable(TODAY);
    }

    @Test
    void 완료한_계획은_삭제할_수_없다() {
        assertErrorCode(() -> doneToday().validateDeletable(TODAY), ErrorCode.PLAN_NOT_MODIFIABLE);
    }

    @Test
    void 실패한_계획은_삭제할_수_없다() {
        assertErrorCode(() -> failedPlan().validateDeletable(TODAY), ErrorCode.PLAN_NOT_MODIFIABLE);
    }

    // ---- 완료 ----

    @Test
    void 오늘_계획은_결과물이_있으면_완료할_수_있다() {
        Plan plan = planWithLink(TODAY);

        plan.complete(45, "집중이 잘 됐다", NOON);

        assertThat(plan.getStatus()).isEqualTo(PlanStatus.DONE);
        assertThat(plan.getActualMinutes()).isEqualTo(45);
        assertThat(plan.getMemo()).isEqualTo("집중이 잘 됐다");
        assertThat(plan.getCompletedAt()).isEqualTo(NOON);
    }

    @Test
    void 결과물이_없으면_완료할_수_없다() {
        Plan plan = planOn(TODAY);

        assertErrorCode(() -> plan.complete(45, null, NOON), ErrorCode.PLAN_EVIDENCE_REQUIRED);
        assertThat(plan.getStatus()).isEqualTo(PlanStatus.PLANNED);
    }

    @Test
    void 내일_계획은_완료할_수_없다() {
        Plan plan = planWithLink(TOMORROW);

        assertErrorCode(() -> plan.complete(45, null, NOON), ErrorCode.PLAN_NOT_COMPLETABLE);
    }

    @Test
    void 어제_계획은_완료할_수_없다() {
        Plan plan = planWithLink(YESTERDAY);

        assertErrorCode(() -> plan.complete(45, null, NOON), ErrorCode.PLAN_NOT_COMPLETABLE);
    }

    @Test
    void 하루가_끝나기_직전까지는_완료할_수_있다() {
        Plan plan = planWithLink(TODAY);

        plan.complete(45, null, TODAY.atTime(LocalTime.MAX));

        assertThat(plan.getStatus()).isEqualTo(PlanStatus.DONE);
    }

    @Test
    void 자정이_지나면_완료할_수_없다() {
        Plan plan = planWithLink(TODAY);

        assertErrorCode(() -> plan.complete(45, null, TOMORROW.atStartOfDay()), ErrorCode.PLAN_NOT_COMPLETABLE);
    }

    @Test
    void 이미_완료한_계획은_다시_완료할_수_없다() {
        assertErrorCode(() -> doneToday().complete(10, null, NOON), ErrorCode.PLAN_NOT_COMPLETABLE);
    }

    @Test
    void 실패한_계획은_완료할_수_없다() {
        assertErrorCode(() -> failedPlan().complete(10, null, NOON), ErrorCode.PLAN_NOT_COMPLETABLE);
    }

    @Test
    void 실제_시간이_0분이면_완료할_수_있고_음수이면_완료할_수_없다() {
        Plan zero = planWithLink(TODAY);
        zero.complete(0, null, NOON);

        assertThat(zero.getActualMinutes()).isZero();
        assertErrorCode(() -> planWithLink(TODAY).complete(-1, null, NOON), ErrorCode.INVALID_ACTUAL_MINUTES);
    }

    @Test
    void 실제_시간이_1440분을_넘으면_완료할_수_없다() {
        assertErrorCode(() -> planWithLink(TODAY).complete(1441, null, NOON), ErrorCode.INVALID_ACTUAL_MINUTES);
    }

    @Test
    void 완료할_때_메모가_비어_있으면_null로_저장한다() {
        Plan plan = planWithLink(TODAY);

        plan.complete(30, "  ", NOON);

        assertThat(plan.getMemo()).isNull();
    }

    // ---- 기록 수정(reflection) ----

    @Test
    void 진행_전_계획은_기록을_수정할_수_없다() {
        assertErrorCode(() -> planOn(TODAY).reflect(null, null, "메모"), ErrorCode.PLAN_REFLECTION_NOT_ALLOWED);
    }

    @Test
    void 완료한_계획은_메모만_고칠_수_있다() {
        Plan plan = doneToday();

        plan.reflect(null, null, "나중에 적은 메모");

        assertThat(plan.getMemo()).isEqualTo("나중에 적은 메모");
        assertThat(plan.getActualMinutes()).isEqualTo(50);
    }

    @Test
    void 완료한_계획의_실제_시간은_고칠_수_없다() {
        assertErrorCode(() -> doneToday().reflect(10, null, null), ErrorCode.PLAN_REFLECTION_NOT_ALLOWED);
    }

    @Test
    void 완료한_계획에는_실패_사유를_적을_수_없다() {
        assertErrorCode(() -> doneToday().reflect(null, MissReason.ETC, null),
                ErrorCode.PLAN_REFLECTION_NOT_ALLOWED);
    }

    @Test
    void 실패한_계획은_시간_사유_메모를_고칠_수_있다() {
        Plan plan = failedPlan();

        plan.reflect(20, MissReason.TOO_BIG, "목표가 너무 컸다");

        assertThat(plan.getActualMinutes()).isEqualTo(20);
        assertThat(plan.getMissReason()).isEqualTo(MissReason.TOO_BIG);
        assertThat(plan.getMemo()).isEqualTo("목표가 너무 컸다");
        assertThat(plan.getStatus()).isEqualTo(PlanStatus.FAILED);
    }

    @Test
    void 실패한_계획의_기록은_값이_없으면_바꾸지_않는다() {
        Plan plan = failedPlan();
        plan.reflect(20, MissReason.SCHEDULE, "일정이 생겼다");

        plan.reflect(null, null, null);

        assertThat(plan.getActualMinutes()).isEqualTo(20);
        assertThat(plan.getMissReason()).isEqualTo(MissReason.SCHEDULE);
        assertThat(plan.getMemo()).isEqualTo("일정이 생겼다");
    }

    @Test
    void 실패한_계획의_실제_시간이_음수이면_고칠_수_없다() {
        assertErrorCode(() -> failedPlan().reflect(-5, null, null), ErrorCode.INVALID_ACTUAL_MINUTES);
    }

    @Test
    void 기록_수정에_실패하면_다른_값도_바뀌지_않는다() {
        Plan plan = failedPlan();

        assertErrorCode(() -> plan.reflect(10, MissReason.ETC, "가".repeat(201)), ErrorCode.PLAN_TEXT_TOO_LONG);

        assertThat(plan.getActualMinutes()).isNull();
        assertThat(plan.getMissReason()).isNull();
    }

    // ---- 자동 실패 ----

    @Test
    void 날짜가_지난_진행_전_계획은_실패로_바뀐다() {
        Plan plan = planOn(YESTERDAY);

        boolean failed = plan.failIfExpired(TODAY);

        assertThat(failed).isTrue();
        assertThat(plan.getStatus()).isEqualTo(PlanStatus.FAILED);
    }

    @Test
    void 여러_날_밀린_계획도_한_번에_실패로_바뀐다() {
        Plan plan = planOn(TODAY.minusDays(5));

        assertThat(plan.failIfExpired(TODAY)).isTrue();
    }

    @Test
    void 오늘_계획은_실패로_바뀌지_않는다() {
        Plan plan = planOn(TODAY);

        assertThat(plan.failIfExpired(TODAY)).isFalse();
        assertThat(plan.getStatus()).isEqualTo(PlanStatus.PLANNED);
    }

    @Test
    void 날짜가_지났어도_완료한_계획은_실패로_바뀌지_않는다() {
        Plan plan = doneToday();

        assertThat(plan.failIfExpired(TOMORROW)).isFalse();
        assertThat(plan.getStatus()).isEqualTo(PlanStatus.DONE);
    }

    @Test
    void 이미_실패한_계획에_다시_실행해도_결과가_같다() {
        Plan plan = planOn(YESTERDAY);
        plan.failIfExpired(TODAY);

        assertThat(plan.failIfExpired(TODAY)).isFalse();
        assertThat(plan.getStatus()).isEqualTo(PlanStatus.FAILED);
    }

    @Test
    void 하루_경계_직전까지는_오늘_계획이고_직후부터는_실패_대상이다() {
        Plan plan = planOn(TODAY);

        assertThat(plan.failIfExpired(TODAY)).isFalse();
        assertThat(plan.failIfExpired(TOMORROW)).isTrue();
    }

    // ---- 결과물 링크 ----

    @Test
    void 링크_결과물을_추가할_수_있다() {
        Plan plan = planOn(TODAY);

        PlanEvidence evidence = plan.addLink(LINK, TODAY);

        assertThat(evidence.getType()).isEqualTo(EvidenceType.URL);
        assertThat(evidence.getUrl()).isEqualTo(LINK);
        assertThat(plan.getEvidences()).containsExactly(evidence);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "ftp://example.com/a", "javascript:alert(1)", "example.com", "https://", "not a url"})
    void 웹_주소가_아닌_링크는_추가할_수_없다(String url) {
        Plan plan = planOn(TODAY);

        assertErrorCode(() -> plan.addLink(url, TODAY), ErrorCode.INVALID_EVIDENCE_URL);
        assertThat(plan.getEvidences()).isEmpty();
    }

    @Test
    void 결과물은_계획당_5개까지_추가할_수_있다() {
        Plan plan = planOn(TODAY);
        for (int i = 0; i < Plan.MAX_EVIDENCES; i++) {
            plan.addLink(LINK + i, TODAY);
        }

        assertErrorCode(() -> plan.addLink(LINK, TODAY), ErrorCode.PLAN_EVIDENCE_LIMIT_EXCEEDED);
        assertThat(plan.getEvidences()).hasSize(Plan.MAX_EVIDENCES);
    }

    @Test
    void 완료한_계획에도_결과물을_더_추가할_수_있다() {
        Plan plan = doneToday();

        plan.addLink("https://blog.example.com/post", TODAY);

        assertThat(plan.getEvidences()).hasSize(2);
    }

    @Test
    void 실패한_계획에는_결과물을_추가할_수_없다() {
        assertErrorCode(() -> failedPlan().addLink(LINK, TODAY), ErrorCode.PLAN_EVIDENCE_NOT_ALLOWED);
    }

    @Test
    void 날짜가_지난_진행_전_계획에는_결과물을_추가할_수_없다() {
        assertErrorCode(() -> planOn(YESTERDAY).addLink(LINK, TODAY), ErrorCode.PLAN_EVIDENCE_NOT_ALLOWED);
    }

    // ---- 결과물 파일 ----

    @Test
    void PDF_파일_결과물을_저장소에_올린다() {
        Plan plan = planOn(TODAY);

        PlanEvidence evidence = plan.addFile("resume.pdf", pdf(), storage, TODAY);

        assertThat(evidence.getType()).isEqualTo(EvidenceType.FILE);
        assertThat(evidence.getFileName()).isEqualTo("resume.pdf");
        assertThat(evidence.getContentType()).isEqualTo("application/pdf");
        assertThat(evidence.getObjectKey()).startsWith("plan-evidences/1/").endsWith(".pdf");
        assertThat(storage.contains(evidence.getObjectKey())).isTrue();
    }

    @Test
    void 이미지_파일_결과물도_올릴_수_있다() {
        PlanEvidence evidence = planOn(TODAY).addFile("shot.png", png(), storage, TODAY);

        assertThat(evidence.getContentType()).isEqualTo("image/png");
        assertThat(evidence.getObjectKey()).endsWith(".png");
    }

    @Test
    void 확장자가_PDF여도_내용이_PDF가_아니면_올릴_수_없다() {
        Plan plan = planOn(TODAY);

        assertErrorCode(() -> plan.addFile("fake.pdf", "hello".getBytes(StandardCharsets.UTF_8), storage, TODAY),
                ErrorCode.INVALID_EVIDENCE_FILE_TYPE);
        assertThat(plan.getEvidences()).isEmpty();
        assertThat(storage.size()).isZero();
    }

    @Test
    void 비어_있는_파일은_올릴_수_없다() {
        assertErrorCode(() -> planOn(TODAY).addFile("a.pdf", new byte[0], storage, TODAY), ErrorCode.EMPTY_FILE);
    }

    @Test
    void 파일이_5MB를_넘으면_올릴_수_없다() {
        byte[] content = new byte[PlanEvidence.MAX_SIZE_BYTES + 1];
        System.arraycopy(pdf(), 0, content, 0, pdf().length);

        assertErrorCode(() -> planOn(TODAY).addFile("big.pdf", content, storage, TODAY), ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    void 파일_이름에서_경로를_제거한다() {
        PlanEvidence evidence = planOn(TODAY).addFile("C:\\docs\\..\\resume.pdf", pdf(), storage, TODAY);

        assertThat(evidence.getFileName()).isEqualTo("resume.pdf");
    }

    @Test
    void 파일_이름이_255자를_넘으면_확장자가_남는_뒤쪽만_저장한다() {
        String longName = "가".repeat(300) + ".pdf";

        PlanEvidence evidence = planOn(TODAY).addFile(longName, pdf(), storage, TODAY);

        assertThat(evidence.getFileName()).hasSize(255).endsWith(".pdf");
    }

    @Test
    void 저장소_업로드에_실패하면_결과물이_추가되지_않는다() {
        Plan plan = planOn(TODAY);
        storage.failOnPut();

        assertErrorCode(() -> plan.addFile("a.pdf", pdf(), storage, TODAY), ErrorCode.STORAGE_FAILED);
        assertThat(plan.getEvidences()).isEmpty();
    }

    @Test
    void 개수_한도가_찬_계획에는_파일도_올릴_수_없고_저장소에도_남지_않는다() {
        Plan plan = planOn(TODAY);
        for (int i = 0; i < Plan.MAX_EVIDENCES; i++) {
            plan.addLink(LINK + i, TODAY);
        }

        assertErrorCode(() -> plan.addFile("a.pdf", pdf(), storage, TODAY), ErrorCode.PLAN_EVIDENCE_LIMIT_EXCEEDED);
        assertThat(storage.size()).isZero();
    }

    @Test
    void 파일_결과물은_내려받는_주소를_만들_수_있다() {
        PlanEvidence evidence = planOn(TODAY).addFile("a.pdf", pdf(), storage, TODAY);

        assertThat(evidence.createDownloadUrl(storage).url()).contains(evidence.getObjectKey());
    }

    @Test
    void 링크_결과물은_내려받는_주소를_만들_수_없다() {
        PlanEvidence link = planOn(TODAY).addLink(LINK, TODAY);

        assertErrorCode(() -> link.createDownloadUrl(storage), ErrorCode.PLAN_EVIDENCE_NOT_FILE);
    }

    @Test
    void 파일_결과물을_지우면_저장소_객체가_삭제된다() {
        PlanEvidence evidence = planOn(TODAY).addFile("a.pdf", pdf(), storage, TODAY);

        evidence.deleteFile(storage);

        assertThat(storage.contains(evidence.getObjectKey())).isFalse();
    }

    // ---- 결과물 삭제 ----

    @Test
    void 진행_전_계획의_결과물을_삭제할_수_있다() {
        Plan plan = planWithLink(TODAY);

        PlanEvidence removed = plan.removeEvidence(100L, TODAY);

        assertThat(removed.getUrl()).isEqualTo(LINK);
        assertThat(plan.getEvidences()).isEmpty();
    }

    @Test
    void 완료한_계획의_마지막_결과물은_삭제할_수_없다() {
        Plan plan = doneToday();

        assertErrorCode(() -> plan.removeEvidence(100L, TODAY), ErrorCode.PLAN_EVIDENCE_REQUIRED);
        assertThat(plan.getEvidences()).hasSize(1);
    }

    @Test
    void 완료한_계획도_결과물이_둘_이상이면_하나를_삭제할_수_있다() {
        Plan plan = doneToday();
        PlanEvidence extra = plan.addLink("https://blog.example.com/post", TODAY);
        ReflectionTestUtils.setField(extra, "id", 101L);

        plan.removeEvidence(101L, TODAY);

        assertThat(plan.getEvidences()).hasSize(1);
    }

    @Test
    void 실패한_계획의_결과물은_삭제할_수_없다() {
        Plan plan = planWithLink(YESTERDAY);
        plan.failIfExpired(TODAY);

        assertErrorCode(() -> plan.removeEvidence(100L, TODAY), ErrorCode.PLAN_NOT_MODIFIABLE);
    }

    @Test
    void 없는_결과물은_삭제할_수_없다() {
        Plan plan = planWithLink(TODAY);

        assertErrorCode(() -> plan.removeEvidence(999L, TODAY), ErrorCode.PLAN_EVIDENCE_NOT_FOUND);
    }
}
