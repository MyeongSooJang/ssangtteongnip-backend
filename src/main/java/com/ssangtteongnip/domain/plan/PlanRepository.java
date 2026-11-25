package com.ssangtteongnip.domain.plan;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PlanRepository {
    Plan save(Plan plan);
    // 새로 추가한 결과물의 id를 응답에 담을 수 있도록 지금까지의 변경을 DB에 반영한다.
    void flush();
    void delete(Plan plan);

    // 남의 계획은 id를 알아도 찾을 수 없도록 사용자까지 함께 조회한다.
    Optional<Plan> findByIdAndUserId(Long id, Long userId);

    // 결과물 개수 한도를 동시 요청이 함께 넘지 못하도록 계획 행을 잠그고 조회한다.
    Optional<Plan> findByIdAndUserIdForUpdate(Long id, Long userId);

    // 날짜 순, 같은 날은 먼저 만든 계획이 먼저
    List<Plan> findInPeriod(Long userId, LocalDate from, LocalDate to);

    // 날짜가 오늘보다 앞서는데 아직 진행 전인 계획 (자동 실패 대상)
    List<Plan> findPlannedBefore(LocalDate today);
}
