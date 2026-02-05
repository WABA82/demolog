package com.examples.demolog.domains.common.idempotency.batch;

import com.examples.demolog.domains.common.idempotency.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyKeyBatchJob {

    private final IdempotencyService idempotencyService;

    /**
     * 만료된 멱등 키를 정리한다.
     * 매일 새벽 2시에 실행된다.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredKeys() {
        try {
            log.info("만료된 멱등 키 정리 배치 작업 시작");
            idempotencyService.deleteExpiredKeys();
            log.info("만료된 멱등 키 정리 배치 작업 완료");
        } catch (Exception e) {
            log.error("만료된 멱등 키 정리 배치 작업 실패", e);
        }
    }
}
