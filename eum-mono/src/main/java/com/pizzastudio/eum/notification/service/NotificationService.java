package com.pizzastudio.eum.notification.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pizzastudio.eum.notification.domain.Notification;
import com.pizzastudio.eum.notification.domain.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 알림 발송.
 *
 * <p>지금은 신청 접수와 같은 트랜잭션 안에서 돈다. 발송기가 예외를 던지면 접수까지
 * 되돌아간다. 4부에서 이 결합을 끊는다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSender sender;

    public Notification notify(String receiverId, String channelId, String title, String content) {
        Notification notification = notificationRepository.save(Notification.builder()
            .receiverId(receiverId)
            .channelId(channelId)
            .title(title)
            .content(content)
            .build());

        sender.send(channelId, receiverId, title, content);
        return notification.markSent();
    }
}
