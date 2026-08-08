package com.pizzastudio.eum.notification.config;

import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pizzastudio.eum.contract.NotificationRequestedEvent;
import com.pizzastudio.eum.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

/**
 * 큐에서 발송 요청을 꺼낸다.
 *
 * <p>브로커를 아는 코드는 이 어댑터 하나뿐이다. 카프카로 바꿔도 여기만 손대면 된다
 * (부록 F).</p>
 */
@Configuration
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Bean
    Consumer<Message<String>> notificationRequested() {
        return message -> {
            // 보내는 쪽이 실은 요청 식별자를 되살린다. 없으면 비워 둔다 — 그 자체가
            // "어디서 온 요청인지 모른다"는 사실이다(19.1).
            Object requestId = message.getHeaders().get("X-Request-ID");
            org.slf4j.MDC.put(com.pizzastudio.eum.notification.common.RequestIdFilter.MDC_KEY,
                requestId == null ? "-" : String.valueOf(requestId));
            try {
                notificationService.handle(
                    objectMapper.readValue(message.getPayload(), NotificationRequestedEvent.class));
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new IllegalArgumentException("이벤트를 읽지 못했습니다. " + message.getPayload(), e);
            } finally {
                org.slf4j.MDC.remove(com.pizzastudio.eum.notification.common.RequestIdFilter.MDC_KEY);
            }
        };
    }
}
