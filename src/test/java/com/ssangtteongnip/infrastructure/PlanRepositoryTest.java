package com.ssangtteongnip.infrastructure;

import com.ssangtteongnip.domain.plan.Plan;
import com.ssangtteongnip.domain.plan.PlanCategory;
import com.ssangtteongnip.domain.plan.PlanDetail;
import com.ssangtteongnip.domain.plan.PlanStatus;
import com.ssangtteongnip.infrastructure.persistence.PlanJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PlanRepositoryTest {

    private static final Long OWNER = 1L;
    private static final Long OTHER = 2L;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 25);
    private static final String LINK = "https://github.com/ssangtteongnip/job-tracker";

    @Autowired PlanJpaRepository planRepository;
    @Autowired EntityManager entityManager;

    private Plan save(Long userId, LocalDate date, String content) {
        PlanDetail detail = new PlanDetail(content, PlanCategory.SKILL, "기준", 30, null, null, null);
        // 날짜 규칙은 도메인 테스트에서 다루므로, 어떤 날짜든 저장할 수 있게 기준일을 계획 날짜로 넘긴다.
        return planRepository.saveAndFlush(Plan.create(userId, date, detail, date));
    }

    private void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void 같은_사용자가_같은_날짜에_여러_계획을_저장할_수_있다() {
        save(OWNER, TODAY, "알고리즘");
        save(OWNER, TODAY, "이력서");

        List<Plan> plans = planRepository.findInPeriod(OWNER, TODAY, TODAY);

        assertThat(plans).extracting(plan -> plan.getDetail().getContent()).containsExactly("알고리즘", "이력서");
    }

    @Test
    void 기간_안의_계획만_날짜_순으로_조회한다() {
        save(OWNER, TODAY.plusDays(1), "내일");
        save(OWNER, TODAY, "오늘");
        save(OWNER, TODAY.minusDays(1), "어제");
        save(OWNER, TODAY.plusDays(8), "범위 밖");

        List<Plan> plans = planRepository.findInPeriod(OWNER, TODAY.minusDays(1), TODAY.plusDays(1));

        assertThat(plans).extracting(plan -> plan.getDetail().getContent()).containsExactly("어제", "오늘", "내일");
    }

    @Test
    void 다른_사용자의_계획은_id를_알아도_조회되지_않는다() {
        Plan others = save(OTHER, TODAY, "남의 계획");

        assertThat(planRepository.findByIdAndUserId(others.getId(), OWNER)).isEmpty();
        assertThat(planRepository.findByIdAndUserIdForUpdate(others.getId(), OWNER)).isEmpty();
        assertThat(planRepository.findInPeriod(OWNER, TODAY.minusDays(30), TODAY.plusDays(30))).isEmpty();
    }

    @Test
    void 본인의_계획은_잠금_조회로도_찾는다() {
        Plan mine = save(OWNER, TODAY, "내 계획");

        assertThat(planRepository.findByIdAndUserIdForUpdate(mine.getId(), OWNER)).isPresent();
    }

    @Test
    void 결과물은_계획과_함께_저장되고_다시_읽는다() {
        Plan plan = save(OWNER, TODAY, "계획");
        plan.addLink(LINK, TODAY);
        planRepository.saveAndFlush(plan);
        clearPersistenceContext();

        Plan found = planRepository.findInPeriod(OWNER, TODAY, TODAY).get(0);

        assertThat(found.getEvidences()).hasSize(1);
        assertThat(found.getEvidences().get(0).getId()).isNotNull();
        assertThat(found.getEvidences().get(0).getUrl()).isEqualTo(LINK);
    }

    @Test
    void 결과물을_뺀_뒤_저장하면_결과물_행도_삭제된다() {
        Plan plan = save(OWNER, TODAY, "계획");
        plan.addLink(LINK, TODAY);
        planRepository.saveAndFlush(plan);
        plan.removeEvidence(plan.getEvidences().get(0).getId(), TODAY);
        planRepository.saveAndFlush(plan);
        clearPersistenceContext();

        assertThat(planRepository.findInPeriod(OWNER, TODAY, TODAY).get(0).getEvidences()).isEmpty();
    }

    @Test
    void 날짜가_오늘보다_앞선_진행_전_계획만_실패_대상으로_조회한다() {
        save(OWNER, TODAY.minusDays(3), "3일 전");
        save(OTHER, TODAY.minusDays(1), "다른 사용자의 어제");
        save(OWNER, TODAY, "오늘");
        save(OWNER, TODAY.plusDays(1), "내일");
        Plan done = save(OWNER, TODAY.minusDays(2), "완료한 계획");
        done.addLink(LINK, TODAY.minusDays(2));
        done.complete(10, null, TODAY.minusDays(2).atTime(10, 0));
        planRepository.saveAndFlush(done);

        List<Plan> targets = planRepository.findPlannedBefore(TODAY);

        assertThat(targets).extracting(plan -> plan.getDetail().getContent())
                .containsExactlyInAnyOrder("3일 전", "다른 사용자의 어제");
        assertThat(targets).allMatch(plan -> plan.getStatus() == PlanStatus.PLANNED);
    }

    @Test
    void 실패_처리한_계획은_다음_조회에서_대상이_아니다() {
        Plan old = save(OWNER, TODAY.minusDays(1), "어제");
        old.failIfExpired(TODAY);
        planRepository.saveAndFlush(old);

        assertThat(planRepository.findPlannedBefore(TODAY)).isEmpty();
    }
}
