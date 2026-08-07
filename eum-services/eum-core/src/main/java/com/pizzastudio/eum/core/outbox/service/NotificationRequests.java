package com.pizzastudio.eum.core.outbox.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pizzastudio.eum.contract.EventNames;
import com.pizzastudio.eum.contract.NotificationRequestedEvent;

import lombok.RequiredArgsConstructor;

/**
 * 알림을 요청한다.
 *
 * <p>모놀리스에서는 발송기를 직접 불렀다. 발송이 실패하면 접수까지 되돌아갔다.
 * 이제는 아웃박스에 적기만 하고 발송은 알림 서비스가 맡는다. 알림이 죽어 있어도
 * 접수는 그대로 끝난다(15장).</p>
 */
@Component
@RequiredArgsConstructor
public class NotificationRequests {

    private final OutboxRecorder outboxRecorder;

    @Transactional(propagation = Propagation.MANDATORY)
    public void request(String receiverId, String channelId, String title, String content) {
        String eventId = java.util.UUID.randomUUID().toString();
        outboxRecorder.record(EventNames.NOTIFICATION_REQUESTED, receiverId,
            new NotificationRequestedEvent(eventId, receiverId, channelId, title, content));
    }
}
