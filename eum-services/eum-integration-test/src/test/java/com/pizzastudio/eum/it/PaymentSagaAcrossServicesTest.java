package com.pizzastudio.eum.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.pizzastudio.eum.core.EumCoreApplication;
import com.pizzastudio.eum.core.application.api.dto.ApplicationSaveRequestDto;
import com.pizzastudio.eum.core.application.domain.ApplicationRepository;
import com.pizzastudio.eum.core.application.service.ApplicationService;
import com.pizzastudio.eum.core.outbox.service.OutboxPublisher;
import com.pizzastudio.eum.core.program.api.dto.ProgramSaveRequestDto;
import com.pizzastudio.eum.core.program.service.ProgramService;
import com.pizzastudio.eum.core.review.api.dto.ReviewRequestDto;
import com.pizzastudio.eum.core.review.service.ReviewService;
import com.pizzastudio.eum.notification.EumNotificationApplication;
import com.pizzastudio.eum.notification.domain.NotificationRepository;
import com.pizzastudio.eum.payment.EumPaymentApplication;
import com.pizzastudio.eum.payment.domain.PaymentRepository;
import com.pizzastudio.eum.payment.outbox.PaymentOutbox;
import com.pizzastudio.eum.payment.service.PaymentService;

/**
 * 본체 · 지급 · 알림을 함께 띄우고 사가 한 바퀴를 확인한다.
 *
 * <p>서비스별 시험은 자기 안에서만 맞으면 통과한다. 본체가 내보낸 메시지를 지급이 읽지
 * 못해도 양쪽 시험은 모두 초록색이다. 규격이 어긋나는 자리는 대개 그 사이다.</p>
 *
 * <p>그래서 여기서는 <b>한쪽이 실제로 내보낸 메시지를 그대로 다른 쪽에 넣는다.</b>
 * 시험이 만든 가짜 메시지가 아니라 발송기가 직렬화해 브로커로 보낸 바로 그 바이트다.
 * 규격이 어긋나면 여기서 터진다.</p>
 *
 * <pre>
 * [본체]  심사 선정 → 아웃박스 기록 → 발송
 *            │  application-approved
 *            ▼
 * [지급]  지시 생성 → 이체 → 아웃박스 기록 → 발송
 *            │  payment-completed / payment-failed
 *            ▼
 * [본체]  지급완료로 확정 · 실패면 접수로 되돌림(보상)
 *            │  notification-requested
 *            ▼
 * [알림]  발송 이력 저장
 * </pre>
 */
@DisplayName("서비스 사이 지급 사가")
class PaymentSagaAcrossServicesTest {

    private static ConfigurableApplicationContext core;
    private static ConfigurableApplicationContext payment;
    private static ConfigurableApplicationContext notification;

    private static OutputDestination coreOut;
    private static InputDestination coreIn;
    private static OutputDestination paymentOut;
    private static InputDestination paymentIn;
    private static InputDestination notificationIn;

    private static ApplicationService applicationService;
    private static ProgramService programService;
    private static ReviewService reviewService;
    private static OutboxPublisher corePublisher;
    private static ApplicationRepository applications;

    private static PaymentService paymentService;
    private static PaymentRepository payments;
    private static PaymentOutbox paymentOutbox;

    private static NotificationRepository notifications;

    @BeforeAll
    static void bootAll() {
        core = ServiceContexts.boot(EumCoreApplication.class, ServiceContexts.CORE);
        payment = ServiceContexts.boot(EumPaymentApplication.class, ServiceContexts.PAYMENT);
        notification = ServiceContexts.boot(EumNotificationApplication.class, ServiceContexts.NOTIFICATION);

        coreOut = ServiceContexts.outbound(core);
        coreIn = ServiceContexts.inbound(core);
        paymentOut = ServiceContexts.outbound(payment);
        paymentIn = ServiceContexts.inbound(payment);
        notificationIn = ServiceContexts.inbound(notification);

        applicationService = core.getBean(ApplicationService.class);
        programService = core.getBean(ProgramService.class);
        reviewService = core.getBean(ReviewService.class);
        corePublisher = core.getBean(OutboxPublisher.class);
        applications = core.getBean(ApplicationRepository.class);

        paymentService = payment.getBean(PaymentService.class);
        payments = payment.getBean(PaymentRepository.class);
        paymentOutbox = payment.getBean(PaymentOutbox.class);

        notifications = notification.getBean(NotificationRepository.class);
    }

    @AfterAll
    static void shutdownAll() {
        SecurityContextHolder.clearContext();
        for (ConfigurableApplicationContext context : List.of(notification, payment, core)) {
            if (context != null) {
                context.close();
            }
        }
    }

    @BeforeEach
    void drainAndLogin() {
        // 앞선 시험이 남긴 메시지를 비운다
        while (coreOut.receive(10, "application-approved") != null) {
            // 버린다
        }
        while (coreOut.receive(10, "notification-requested") != null) {
            // 버린다
        }
        while (paymentOut.receive(10, "payment-completed") != null) {
            // 버린다
        }
        while (paymentOut.receive(10, "payment-failed") != null) {
            // 버린다
        }
        loginAs("admin", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("선정하면 지급이 지시를 만들고, 이체가 끝나면 본체가 지급완료로 확정한다")
    void happyPath() {
        String applicationId = applyAndApprove("110-000-111111");

        // 본체 → 지급
        Message<byte[]> approved = publishAndTake(coreOut, "application-approved");
        assertThat(approved).as("본체가 선정 이벤트를 내보내야 한다").isNotNull();
        paymentIn.send(forward(approved), "application-approved");

        assertThat(payments.findByApplicationId(applicationId))
            .as("지급이 지시를 만들어야 한다")
            .isPresent();

        // 지급: 이체 → 결과 발행
        assertThat(paymentService.executeReady()).isEqualTo(1);
        assertThat(paymentOutbox.publishPending()).isEqualTo(1);

        Message<byte[]> completed = paymentOut.receive(2000, "payment-completed");
        assertThat(completed).as("지급이 완료 이벤트를 내보내야 한다").isNotNull();

        // 지급 → 본체
        coreIn.send(forward(completed), "payment-completed");

        assertThat(applications.findById(applicationId))
            .get()
            .extracting(a -> a.getStatusId())
            .as("본체가 지급완료로 확정해야 한다")
            .isEqualTo("paid");
    }

    @Test
    @DisplayName("이체가 실패하면 본체가 선정을 되돌린다")
    void compensates() {
        // 계좌번호가 비어 있으면 실습용 이체 연계가 실패로 답한다
        String applicationId = applyAndApprove("");

        Message<byte[]> approved = publishAndTake(coreOut, "application-approved");
        paymentIn.send(forward(approved), "application-approved");

        assertThat(paymentService.executeReady()).as("이체가 실패해야 한다").isZero();
        assertThat(paymentOutbox.publishPending()).isEqualTo(1);

        Message<byte[]> failed = paymentOut.receive(2000, "payment-failed");
        assertThat(failed).as("지급이 실패 이벤트를 내보내야 한다").isNotNull();

        coreIn.send(forward(failed), "payment-failed");

        assertThat(applications.findById(applicationId))
            .get()
            .extracting(a -> a.getStatusId())
            .as("선정이 접수로 되돌아가야 한다")
            .isEqualTo("request");
    }

    @Test
    @DisplayName("같은 선정 이벤트가 두 번 와도 지급 지시는 하나다")
    void idempotentAcrossServices() {
        String applicationId = applyAndApprove("110-000-222222");

        Message<byte[]> approved = publishAndTake(coreOut, "application-approved");
        paymentIn.send(forward(approved), "application-approved");
        paymentIn.send(forward(approved), "application-approved");

        assertThat(payments.findAll().stream()
            .filter(p -> applicationId.equals(p.getApplicationId()))
            .count())
            .as("브로커가 두 번 전달해도 지시는 하나여야 한다")
            .isEqualTo(1);
    }

    @Test
    @DisplayName("본체가 낸 알림 요청을 알림 서비스가 읽어 저장한다")
    void notificationCrossesTheBoundary() {
        long before = notifications.count();

        applyAndApprove("110-000-333333");
        corePublisher.publishPending();

        Message<byte[]> requested = coreOut.receive(2000, "notification-requested");
        assertThat(requested).as("본체가 알림 요청을 내보내야 한다").isNotNull();

        notificationIn.send(forward(requested), "notification-requested");

        assertThat(notifications.count())
            .as("알림 서비스가 규격을 읽고 이력을 남겨야 한다")
            .isEqualTo(before + 1);
    }

    // ----- 거들기 -----

    /** 신청을 넣고 선정까지 한다 */
    private String applyAndApprove(String accountNo) {
        Long programId = programService.save(ProgramSaveRequestDto.builder()
            .programName("통합시험 지원사업")
            .categoryId("CAT001")
            .totalBudget(50_000_000L)
            .maxAmountPerCase(5_000_000L)
            .requestStartDate(LocalDateTime.now().minusDays(1))
            .requestEndDate(LocalDateTime.now().plusDays(30))
            .build()).getProgramId();

        loginAs("user1", "ROLE_USER");
        String applicationId = applicationService.apply(ApplicationSaveRequestDto.builder()
            .programId(programId)
            .amount(3_000_000L)
            .purposeContent("운영자금")
            .accountNo(accountNo)
            .bizNo("123-45-67890")
            .build()).getApplicationId();

        loginAs("admin", "ROLE_ADMIN");
        reviewService.review(applicationId,
            ReviewRequestDto.builder().resultId("approve").opinion("적격").build());
        return applicationId;
    }

    /** 아웃박스를 발송한 뒤 그 목적지에서 메시지를 꺼낸다 */
    private Message<byte[]> publishAndTake(OutputDestination out, String destination) {
        corePublisher.publishPending();
        return out.receive(2000, destination);
    }

    /**
     * 한쪽이 내보낸 메시지를 다른 쪽에 넣을 형태로 옮긴다.
     *
     * <p>본문은 손대지 않는다. 브로커를 거쳤을 때와 같도록 전달 헤더만 옮긴다.</p>
     */
    private Message<byte[]> forward(Message<byte[]> received) {
        MessageBuilder<byte[]> builder = MessageBuilder.withPayload(received.getPayload());
        copyHeader(received, builder, "eventId");
        copyHeader(received, builder, "eventName");
        return builder.setHeader("contentType", "application/json").build();
    }

    private void copyHeader(Message<byte[]> from, MessageBuilder<byte[]> to, String name) {
        Object value = from.getHeaders().get(name);
        if (value != null) {
            to.setHeader(name, value);
        }
    }

    private void loginAs(String memberId, String role) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(memberId, "N/A",
                List.of(new SimpleGrantedAuthority(role))));
    }

    @SuppressWarnings("unused")
    private static String newEventId() {
        return UUID.randomUUID().toString();
    }
}
