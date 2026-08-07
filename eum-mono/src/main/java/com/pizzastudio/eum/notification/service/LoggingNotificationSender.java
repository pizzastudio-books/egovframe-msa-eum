package com.pizzastudio.eum.notification.service;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 실습용 발송기. 실제로 보내지 않고 기록만 남긴다.
 */
@Slf4j
@Component
public class LoggingNotificationSender implements NotificationSender {

    @Override
    public void send(String channelId, String receiver, String title, String content) {
        log.info("[{}] to={} title={} content={}", channelId, receiver, title, content);
    }
}
