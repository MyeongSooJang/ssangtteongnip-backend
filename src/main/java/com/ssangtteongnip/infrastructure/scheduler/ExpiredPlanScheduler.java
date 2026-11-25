package com.ssangtteongnip.infrastructure.scheduler;

import com.ssangtteongnip.application.service.PlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 날짜가 지났는데 끝내지 못한 계획을 실패로 바꾼다.
// 서버가 KST 00:00~05:50에 꺼지므로 자정 크론만으로는 돌지 못한다. 기동 직후에도 한 번 돌려 놓친 분량을 바로 정리한다.
// 조건이 "날짜가 오늘보다 앞선 진행 전 계획"이라 놓친 날이 있어도 다음 실행에서 함께 처리되고, 여러 번 돌려도 결과가 같다.
// TODO: 서버를 여러 대로 늘리면 같은 계획을 동시에 처리하지 않도록 분산 락이 필요하다. (지금은 단일 서버)
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredPlanScheduler {

    private final PlanService planService;

    @EventListener(ApplicationReadyEvent.class)
    public void failExpiredPlansOnStartup() {
        failExpiredPlans();
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void failExpiredPlansAtMidnight() {
        failExpiredPlans();
    }

    private void failExpiredPlans() {
        int failed = planService.failExpiredPlans();
        log.info("날짜가 지난 진행 전 계획 {}건을 실패 처리했다", failed);
    }
}
