package com.pizzastudio.eum.core.saga;

import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pizzastudio.eum.contract.PaymentCompletedEvent;
import com.pizzastudio.eum.contract.PaymentFailedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 지급 결과를 받아 신청 상태를 맞춘다.
 *
 * <p>여기가 사가의 되돌아오는 쪽이다. 지급이 끝나면 지급완료로, 실패하면 선정을
 * 되돌린다(보상). 브로커를 아는 코드는 이 어댑터와 아웃박스 발송기 둘뿐이다.</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class PaymentResultListener {

    private final PaymentResultHandler handler;
    private final ObjectMapper objectMapper;

    @Bean
    Consumer<Message<String>> paymentCompleted() {
        return message -> {
            PaymentCompletedEvent event = read(message, PaymentCompletedEvent.class);
            handler.onCompleted(event);
        };
    }

    @Bean
    Consumer<Message<String>> paymentFailed() {
        return message -> {
            PaymentFailedEvent event = read(message, PaymentFailedEvent.class);
            handler.onFailed(event);
        };
    }

    private <T> T read(Message<String> message, Class<T> type) {
        try {
            return objectMapper.readValue(message.getPayload(), type);
        } catch (Exception e) {
            throw new IllegalArgumentException("이벤트를 읽지 못했습니다. " + message.getPayload(), e);
        }
    }
}
