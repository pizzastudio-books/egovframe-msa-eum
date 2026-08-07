package com.pizzastudio.eum.core.outbox.domain;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /** 아직 보내지 않은 것을 만들어진 순서대로 */
    List<OutboxEvent> findByPublishedAtIsNullOrderByOutboxId(Limit limit);

    boolean existsByEventId(String eventId);
}
