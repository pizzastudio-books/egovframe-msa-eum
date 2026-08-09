package com.pizzastudio.eum.core.outbox.domain;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /** 아직 보내지 않은 것을 만들어진 순서대로 */
    List<OutboxEvent> findByPublishedAtIsNullOrderByOutboxId(Limit limit);

    boolean existsByEventId(String eventId);

    /**
     * 아직 못 보낸 건수.
     *
     * <p>경보가 이 값을 본다(19.2). 브로커가 죽거나 발행기가 멈추면 여기가 쌓이는데,
     * <b>접수는 계속 되므로 사용자는 아무것도 못 느낀다.</b> 알림만 안 간다.</p>
     */
    long countByPublishedAtIsNull();
}
