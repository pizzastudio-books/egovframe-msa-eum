package com.pizzastudio.eum.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pizzastudio.eum.contract.ApplicationApprovedEvent;
import com.pizzastudio.eum.contract.PaymentCompletedEvent;
import com.pizzastudio.eum.contract.PaymentFailedEvent;
import com.pizzastudio.eum.payment.domain.PaymentRepository;
import com.pizzastudio.eum.payment.domain.PaymentStatus;
import com.pizzastudio.eum.payment.outbox.PaymentOutbox;
import com.pizzastudio.eum.payment.service.PaymentService;

/**
 * 지급 서비스 통합 시험 — 사가의 가는 쪽과 오는 쪽.
 *
 * <p>RabbitMQ 없이 시험 바인더로 돌린다. 선정 이벤트를 넣으면 지급 지시가 생기고,
 * 이체 결과가 아웃박스를 거쳐 밖으로 나가는지 확인한다.</p>
 */
@SpringBootTest
@Import(TestChannelBinderConfiguration.class)
@ActiveProfiles("test")
class PaymentSagaTest {

    @Autowired
    private InputDestination input;

    @Autowired
    private OutputDestination output;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentOutbox outbox;

    @Autowired
    private ObjectMapper objectMapper;

    private String 선정_이벤트를_보낸다(String accountNo) throws Exception {
        String applicationId = "app-" + UUID.randomUUID();
        ApplicationApprovedEvent event = new ApplicationApprovedEvent(
            UUID.randomUUID().toString(), applicationId, "user1", 1_000_000L, accountNo);
        input.send(MessageBuilder
            .withPayload(objectMapper.writeValueAsString(event)).build(), "application-approved");
        return applicationId;
    }

    @Test
    @DisplayName("선정 이벤트를 받으면 지급 지시가 생긴다")
    void 지급_지시_생성() throws Exception {
        String applicationId = 선정_이벤트를_보낸다("110-000-000000");

        assertThat(paymentRepository.findByApplicationId(applicationId)).isPresent();
        assertThat(paymentRepository.findByApplicationId(applicationId).orElseThrow().getStatusId())
            .isEqualTo(PaymentStatus.READY.getKey());
    }

    @Test
    @DisplayName("같은 선정 이벤트가 두 번 와도 지시는 하나다")
    void 지급_지시_멱등() throws Exception {
        String applicationId = "app-" + UUID.randomUUID();
        ApplicationApprovedEvent event = new ApplicationApprovedEvent(
            UUID.randomUUID().toString(), applicationId, "user1", 1_000_000L, "110-000-000000");
        String payload = objectMapper.writeValueAsString(event);

        input.send(MessageBuilder.withPayload(payload).build(), "application-approved");
        input.send(MessageBuilder.withPayload(payload).build(), "application-approved");

        assertThat(paymentRepository.findAll().stream()
            .filter(p -> p.getApplicationId().equals(applicationId)).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("이체에 성공하면 지급완료 이벤트가 나간다")
    void 지급_완료_이벤트() throws Exception {
        String applicationId = 선정_이벤트를_보낸다("110-000-000000");

        assertThat(paymentService.executeReady()).isEqualTo(1);
        assertThat(outbox.publishPending()).isEqualTo(1);

        Message<byte[]> sent = output.receive(2000, "payment-completed");
        assertThat(sent).isNotNull();
        PaymentCompletedEvent event =
            objectMapper.readValue(new String(sent.getPayload()), PaymentCompletedEvent.class);
        assertThat(event.applicationId()).isEqualTo(applicationId);
        assertThat(event.amount()).isEqualTo(1_000_000L);
    }

    @Test
    @DisplayName("이체에 실패하면 지급실패 이벤트가 나가 본체가 되돌릴 수 있다")
    void 지급_실패_보상() throws Exception {
        String applicationId = 선정_이벤트를_보낸다("");

        assertThat(paymentService.executeReady()).isZero();
        assertThat(outbox.publishPending()).isEqualTo(1);

        Message<byte[]> sent = output.receive(2000, "payment-failed");
        assertThat(sent).isNotNull();
        PaymentFailedEvent event =
            objectMapper.readValue(new String(sent.getPayload()), PaymentFailedEvent.class);
        assertThat(event.applicationId()).isEqualTo(applicationId);
        assertThat(event.reason()).contains("이체 실패");

        assertThat(paymentRepository.findByApplicationId(applicationId).orElseThrow().getStatusId())
            .isEqualTo(PaymentStatus.FAILED.getKey());
    }
}
