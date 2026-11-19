package com.ssangtteongnip.infrastructure.scheduler;

import com.ssangtteongnip.application.service.CompanyCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 스케줄링 활성화는 ExpiredJobApplicationScheduler가 한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyCatalogRefresher {

    private final CompanyCatalogService companyCatalogService;

    // 첫 배포 직후 카탈로그가 비어 있으면 주간 갱신을 기다리지 않고 채운다.
    // 목록이 수만 건이라 오래 걸리므로 기동을 막지 않게 별도 스레드에서 돌린다.
    @EventListener(ApplicationReadyEvent.class)
    public void loadWhenEmpty() {
        Thread.ofVirtual().name("company-catalog-initial-load").start(() -> {
            if (companyCatalogService.isEmpty()) {
                refresh();
            }
        });
    }

    // 서버가 KST 00:00~05:50에 꺼지므로 그 이후 시각으로 잡는다.
    @Scheduled(cron = "0 30 7 * * SUN", zone = "Asia/Seoul")
    public void refreshWeekly() {
        refresh();
    }

    // 실패해도 이전 카탈로그로 검색은 계속 되므로 로그만 남기고 다음 주기를 기다린다.
    private void refresh() {
        try {
            int added = companyCatalogService.refresh();
            log.info("회사 카탈로그 {}건을 새로 추가했다", added);
        } catch (RuntimeException e) {
            log.warn("회사 카탈로그 갱신 실패", e);
        }
    }
}
