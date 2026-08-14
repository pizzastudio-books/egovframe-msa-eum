package com.pizzastudio.eum.batch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.pizzastudio.eum.payment.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 야간 지급 배치.
 *
 * <p>쿼츠나 스프링 배치를 쓰지 않고 {@code @Scheduled} 와 기본 스레드풀로만 돈다. 공공에서
 * 흔한 형태다. <b>이 방식은 프로세스를 여러 개로 늘리는 순간 같은 배치가 중복 실행된다.</b>
 * 11.5 에서 이 문제를 다루고, 배치를 앱에서 떼어 CronJob 으로 옮긴다.</p>
 *
 * <p>그때를 대비해 스케줄을 끌 수 있게 두었다. {@code eum.batch.payment.enabled=false}
 * 로 두면 앱은 배치를 돌리지 않는다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "eum.batch.payment.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentBatch {

    private final PaymentService paymentService;

    @Value("${eum.batch.payment.limit:500}")
    private int limit;

    /** 매일 새벽 2시 */
    @Scheduled(cron = "${eum.batch.payment.cron:0 0 2 * * *}")
    public void run() {
        execute(paymentService, limit);
    }

    /**
     * 배치 한 번 실행. CronJob 으로 옮긴 뒤에도 같은 코드를 쓴다.
     */
    static void execute(PaymentService paymentService, int limit) {
        log.info("야간 지급 배치 시작");
        int created = paymentService.createInstructions(limit);
        int done = paymentService.executeReady();
        log.info("야간 지급 배치 종료 — 지시 {}건 생성, {}건 지급 완료", created, done);
    }
}
