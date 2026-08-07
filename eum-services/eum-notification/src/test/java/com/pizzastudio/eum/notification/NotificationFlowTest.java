package com.pizzastudio.eum.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pizzastudio.eum.contract.NotificationRequestedEvent;
import com.pizzastudio.eum.notification.domain.NotificationRepository;

/**
 * 알림 서비스 통합 시험.
 *
 * <p>RabbitMQ 를 띄우지 않는다. Spring Cloud Stream 의 시험 바인더가 큐 대신
 * 메시지를 넣어 준다. 브로커에 매달지 않고도 리스너·멱등·저장을 확인할 수 있다.</p>
 */
@SpringBootTest
@Import(TestChannelBinderConfiguration.class)
@ActiveProfiles("test")
class NotificationFlowTest {

    @Autowired
    private InputDestination input;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private void 요청을_보낸다(NotificationRequestedEvent event) throws Exception {
        input.send(MessageBuilder
            .withPayload(objectMapper.writeValueAsString(event))
            .build(), "notification-requested");
    }

    @Test
    @DisplayName("발송 요청을 받으면 저장하고 보낸다")
    void 발송_처리() throws Exception {
        long before = notificationRepository.count();
        String eventId = UUID.randomUUID().toString();

        요청을_보낸다(new NotificationRequestedEvent(
            eventId, "user1", "email", "접수되었습니다", "신청이 접수되었습니다"));

        assertThat(notificationRepository.count()).isEqualTo(before + 1);
        assertThat(notificationRepository.existsByEventId(eventId)).isTrue();
        assertThat(notificationRepository.findByReceiverIdOrderByNotificationIdDesc("user1").get(0)
            .getStatusId()).isEqualTo("sent");
    }

    @Test
    @DisplayName("같은 이벤트가 두 번 와도 한 번만 보낸다")
    void 멱등_처리() throws Exception {
        long before = notificationRepository.count();
        String eventId = UUID.randomUUID().toString();
        NotificationRequestedEvent event = new NotificationRequestedEvent(
            eventId, "user2", "sms", "선정되었습니다", "심사 결과 선정되었습니다");

        요청을_보낸다(event);
        요청을_보낸다(event);

        assertThat(notificationRepository.count()).isEqualTo(before + 1);
    }
}
