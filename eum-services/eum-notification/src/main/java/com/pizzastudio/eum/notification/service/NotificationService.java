package com.pizzastudio.eum.notification.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pizzastudio.eum.contract.NotificationRequestedEvent;
import com.pizzastudio.eum.notification.domain.Notification;
import com.pizzastudio.eum.notification.domain.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 발송 처리.
 *
 * <p>같은 이벤트가 두 번 오면 두 번째는 아무것도 하지 않는다. 브로커가 한 번 이상
 * 전달을 보장할 뿐이라 받는 쪽이 멱등해야 한다(18.4).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSender sender;

    /**
     * @return 실제로 보냈으면 true, 이미 처리한 이벤트라 건너뛰었으면 false
     */
    public boolean handle(NotificationRequestedEvent event) {
        if (notificationRepository.existsByEventId(event.eventId())) {
            log.debug("이미 보낸 알림입니다. eventId={}", event.eventId());
            return false;
        }

        Notification notification = notificationRepository.save(Notification.builder()
            .eventId(event.eventId())
            .receiverId(event.receiverId())
            .channelId(event.channelId())
            .title(event.title())
            .content(event.content())
            .build());

        try {
            sender.send(event.channelId(), event.receiverId(), event.title(), event.content());
            notification.markSent();
            return true;
        } catch (RuntimeException e) {
            // 발송에 실패해도 접수는 이미 끝났다. 여기서 실패를 기록하고 다시 시도한다.
            log.warn("발송 실패 eventId={} 이유={}", event.eventId(), e.getMessage());
            notification.markFailed();
            throw e;
        }
    }
}
