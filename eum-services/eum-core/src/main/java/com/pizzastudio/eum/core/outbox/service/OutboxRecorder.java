package com.pizzastudio.eum.core.outbox.service;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pizzastudio.eum.core.outbox.domain.OutboxEvent;
import com.pizzastudio.eum.core.outbox.domain.OutboxEventRepository;

import lombok.RequiredArgsConstructor;

/**
 * 이벤트를 아웃박스에 적는다.
 *
 * <p>부르는 쪽의 트랜잭션에 참여한다({@code MANDATORY}). 업무 자료가 되돌아가면
 * 이벤트도 함께 되돌아가야 하기 때문이다.</p>
 */
@Component
@RequiredArgsConstructor
public class OutboxRecorder {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public String record(String eventName, String aggregateId, Object payload) {
        String eventId = UUID.randomUUID().toString();
        outboxEventRepository.save(OutboxEvent.builder()
            .eventId(eventId)
            .eventName(eventName)
            .aggregateId(aggregateId)
            .payload(serialize(payload))
            .build());
        return eventId;
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("이벤트를 직렬화하지 못했습니다.", e);
        }
    }
}
