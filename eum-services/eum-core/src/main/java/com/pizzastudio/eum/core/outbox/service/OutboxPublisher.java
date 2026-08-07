package com.pizzastudio.eum.core.outbox.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.domain.Limit;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.pizzastudio.eum.core.outbox.domain.OutboxEvent;
import com.pizzastudio.eum.core.outbox.domain.OutboxEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 아웃박스를 읽어 브로커로 보낸다.
 *
 * <p><b>브로커를 아는 코드는 이 한 곳뿐이다.</b> 업무 코드는 아웃박스에 적기만 한다.
 * 그래서 RabbitMQ 를 카프카로 바꿔도 손댈 곳이 발송기와 리스너 어댑터 둘로 좁혀진다
 * (부록 F).</p>
 *
 * <p>발행에 실패하면 그대로 둔다. 다음 차례에 다시 집는다. 같은 이벤트가 두 번 갈 수
 * 있으므로 받는 쪽이 멱등해야 한다(18.4).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final StreamBridge streamBridge;

    @Value("${eum.outbox.batch-size:100}")
    private int batchSize;

    /**
     * 보내지 않은 이벤트를 한 묶음 보낸다.
     *
     * @return 보낸 건수
     */
    @Transactional
    public int publishPending() {
        List<OutboxEvent> pending =
            outboxEventRepository.findByPublishedAtIsNullOrderByOutboxId(Limit.of(batchSize));

        int sent = 0;
        for (OutboxEvent event : pending) {
            event.markAttempted();
            try {
                streamBridge.send(bindingOf(event.getEventName()),
                    MessageBuilder.withPayload(event.getPayload())
                        .setHeader("eventId", event.getEventId())
                        .setHeader("eventName", event.getEventName())
                        .setHeader("contentType", "application/json")
                        .build());
                event.markPublished();
                sent++;
            } catch (RuntimeException e) {
                // 여기서 멈추지 않는다. 다음 차례에 다시 집는다.
                log.warn("이벤트 발행 실패 eventId={} name={} 시도={} 이유={}",
                    event.getEventId(), event.getEventName(), event.getAttemptCount(), e.getMessage());
            }
        }
        return sent;
    }

    /** 이벤트 이름을 Spring Cloud Stream 바인딩 이름으로 바꾼다 */
    static String bindingOf(String eventName) {
        return eventName + "-out-0";
    }
}
