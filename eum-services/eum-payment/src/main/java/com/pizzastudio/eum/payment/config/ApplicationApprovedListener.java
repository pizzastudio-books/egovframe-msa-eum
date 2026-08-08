package com.pizzastudio.eum.payment.config;

import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pizzastudio.eum.contract.ApplicationApprovedEvent;
import com.pizzastudio.eum.payment.service.PaymentService;

import lombok.RequiredArgsConstructor;

/** 선정 이벤트를 받는 어댑터. */
@Configuration
@RequiredArgsConstructor
public class ApplicationApprovedListener {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @Bean
    Consumer<Message<String>> applicationApproved() {
        return message -> {
            // 보내는 쪽이 실은 요청 식별자를 되살린다. 없으면 비워 둔다 — 그 자체가
            // "어디서 온 요청인지 모른다"는 사실이다(19.1).
            Object requestId = message.getHeaders().get("X-Request-ID");
            org.slf4j.MDC.put(com.pizzastudio.eum.payment.common.RequestIdFilter.MDC_KEY,
                requestId == null ? "-" : String.valueOf(requestId));
            try {
                paymentService.createInstruction(
                    objectMapper.readValue(message.getPayload(), ApplicationApprovedEvent.class));
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new IllegalArgumentException("이벤트를 읽지 못했습니다. " + message.getPayload(), e);
            } finally {
                org.slf4j.MDC.remove(com.pizzastudio.eum.payment.common.RequestIdFilter.MDC_KEY);
            }
        };
    }
}
