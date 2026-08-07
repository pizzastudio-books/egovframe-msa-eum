package com.pizzastudio.eum.core.outbox.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 아웃박스 발송기를 주기적으로 깨운다.
 *
 * <p>파드를 여러 벌로 늘리면 여럿이 같은 아웃박스를 집는다. 그래도 받는 쪽이 멱등하면
 * 결과는 같다. 정확히 한 번을 보장하려고 잠금을 거는 대신, 두 번 와도 괜찮게 만드는
 * 쪽을 골랐다(18.4).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "eum.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxScheduler {

    private final OutboxPublisher outboxPublisher;

    @Scheduled(fixedDelayString = "${eum.outbox.interval-millis:2000}")
    public void publish() {
        int sent = outboxPublisher.publishPending();
        if (sent > 0) {
            log.debug("아웃박스 {}건 발행", sent);
        }
    }
}
