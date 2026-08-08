package com.pizzastudio.eum.payment.outbox;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.domain.Limit;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 지급 결과를 적고, 주기적으로 브로커로 보낸다.
 *
 * <p>브로커를 아는 코드는 이 한 곳이다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOutbox {

    private final OutboxEventRepository outboxEventRepository;
    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;

    @Value("${eum.outbox.batch-size:100}")
    private int batchSize;

    /** 업무 트랜잭션 안에서 적는다 */
    @Transactional(propagation = Propagation.MANDATORY)
    public String record(String eventName, String aggregateId, Object payload) {
        String eventId = UUID.randomUUID().toString();
        try {
            outboxEventRepository.save(OutboxEvent.builder()
                .eventId(eventId)
                .eventName(eventName)
                .aggregateId(aggregateId)
                .requestId(org.slf4j.MDC.get(
                com.pizzastudio.eum.payment.common.RequestIdFilter.MDC_KEY))
            .payload(objectMapper.writeValueAsString(payload))
                .build());
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("이벤트를 직렬화하지 못했습니다.", e);
        }
        return eventId;
    }

    @Transactional
    public int publishPending() {
        List<OutboxEvent> pending =
            outboxEventRepository.findByPublishedAtIsNullOrderByOutboxId(Limit.of(batchSize));
        int sent = 0;
        for (OutboxEvent event : pending) {
            event.markAttempted();
            try {
                streamBridge.send(event.getEventName() + "-out-0",
                    MessageBuilder.withPayload(event.getPayload())
                        .setHeader("eventId", event.getEventId())
                        // 요청 식별자를 함께 실어 보낸다. 받는 쪽이 이것을 MDC 에 넣으면
                        // 비동기 흐름까지 한 요청으로 이어진다(19.1).
                        .setHeader("X-Request-ID",
                            event.getRequestId() == null ? "" : event.getRequestId())
                        .setHeader("contentType", "application/json")
                        .build());
                event.markPublished();
                sent++;
            } catch (RuntimeException e) {
                log.warn("이벤트 발행 실패 eventId={} 이유={}", event.getEventId(), e.getMessage());
            }
        }
        return sent;
    }

    @Component
    @RequiredArgsConstructor
    @ConditionalOnProperty(name = "eum.outbox.enabled", havingValue = "true", matchIfMissing = true)
    static class Scheduler {

        private final PaymentOutbox outbox;

        @Scheduled(fixedDelayString = "${eum.outbox.interval-millis:2000}")
        void publish() {
            outbox.publishPending();
        }
    }
}
