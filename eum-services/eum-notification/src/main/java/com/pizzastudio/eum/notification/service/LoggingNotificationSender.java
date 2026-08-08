package com.pizzastudio.eum.notification.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 실습용 발송기. 실제로 보내지 않고 기록만 남긴다.
 *
 * <p>{@code eum.notification.fail-rate} 를 1.0 으로 두면 발송이 늘 실패한다. 15.4 의
 * 재시도와 죽은편지 큐를 눈으로 확인할 때 쓴다. 기본값은 0 이라 평소에는 늘 성공한다.</p>
 */
@Slf4j
@Component
public class LoggingNotificationSender implements NotificationSender {

    private final double failRate;

    public LoggingNotificationSender(
            @Value("${eum.notification.fail-rate:0}") double failRate) {
        this.failRate = failRate;
    }

    @Override
    public void send(String channelId, String receiver, String title, String content) {
        if (failRate > 0 && Math.random() < failRate) {
            throw new IllegalStateException("문자 사업자 응답 없음 (실습용 강제 실패)");
        }
        log.info("[{}] to={} title={} content={}", channelId, receiver, title, content);
    }
}
