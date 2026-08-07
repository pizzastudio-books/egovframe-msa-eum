package com.pizzastudio.eum.payment.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 야간 지급 배치.
 *
 * <p>모놀리스에서는 본체 안에서 돌았다. 지급을 떼면서 이 서비스로 함께 옮겼다.
 * 쿠버네티스에서는 CronJob 이 부르므로 앱 안의 스케줄을 끌 수 있다(11.5).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "eum.batch.payment.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentBatch {

    private final PaymentService paymentService;

    @Scheduled(cron = "${eum.batch.payment.cron:0 0 2 * * *}")
    public void run() {
        log.info("야간 지급 배치 시작");
        int done = paymentService.executeReady();
        log.info("야간 지급 배치 종료 — {}건 지급 완료", done);
    }
}
