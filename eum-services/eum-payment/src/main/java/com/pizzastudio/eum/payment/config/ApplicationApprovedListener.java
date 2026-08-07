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
            try {
                paymentService.createInstruction(
                    objectMapper.readValue(message.getPayload(), ApplicationApprovedEvent.class));
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new IllegalArgumentException("이벤트를 읽지 못했습니다. " + message.getPayload(), e);
            }
        };
    }
}
