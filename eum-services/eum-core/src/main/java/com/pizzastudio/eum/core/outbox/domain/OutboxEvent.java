package com.pizzastudio.eum.core.outbox.domain;

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
 * 아직 브로커로 보내지 않은 이벤트.
 *
 * <p><b>업무 자료와 같은 트랜잭션으로 저장한다.</b> 저장은 됐는데 발행 전에 프로세스가
 * 죽어 이벤트가 사라지는 틈을 막기 위해서다. 템플릿은 저장 직후 곧바로 발행하기 때문에
 * 이 틈이 열려 있다(18.1·18.2).</p>
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

    /** 같은 이벤트가 두 번 와도 결과가 같도록 하는 열쇠 */
    @Column(name = "event_id", length = 40, nullable = false, unique = true)
    private String eventId;

    /** 보낼 곳 — EventNames 의 값 */
    @Column(name = "event_name", length = 100, nullable = false)
    private String eventName;

    /** 이 이벤트가 가리키는 업무 자료 */
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

    public boolean isPublished() {
        return publishedAt != null;
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
