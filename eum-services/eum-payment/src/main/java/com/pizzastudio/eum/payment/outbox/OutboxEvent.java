package com.pizzastudio.eum.payment.outbox;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지급 결과를 본체에 돌려주기 전에 담아 두는 곳.
 *
 * <p>본체와 같은 이유로 아웃박스를 쓴다. 이체는 끝났는데 결과 발행 전에 죽으면
 * 본체는 영원히 선정 상태로 남는다(18.2).</p>
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbox_id")
    private Long outboxId;

    @Column(name = "event_id", length = 40, nullable = false, unique = true)
    private String eventId;

    @Column(name = "event_name", length = 100, nullable = false)
    private String eventName;

    @Column(name = "aggregate_id", length = 40)
    private String aggregateId;

    @Column(name = "payload", length = 4000, nullable = false)
    private String payload;

    /**
     * 이 이벤트를 만든 요청의 식별자(19.1).
     *
     * <p>비동기 흐름은 여기서 끊긴다 — 발행은 2초 뒤 다른 스레드에서 일어나므로 요청의
     * MDC 가 남아 있지 않다. 그래서 값을 표에 적어 두었다가 발행할 때 헤더에 실어 보낸다.</p>
     */
    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public OutboxEvent(String eventId, String eventName, String aggregateId, String payload,
        String requestId) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.requestId = requestId;
        this.attemptCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    public OutboxEvent markPublished() {
        this.publishedAt = LocalDateTime.now();
        return this;
    }

    public OutboxEvent markAttempted() {
        this.attemptCount++;
        return this;
    }
}
