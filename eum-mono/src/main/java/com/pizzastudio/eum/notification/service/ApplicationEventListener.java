package com.pizzastudio.eum.notification.service;

import com.pizzastudio.eum.application.domain.ApplicationEvents;
import com.pizzastudio.eum.payment.domain.PaymentEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 다른 모듈이 알린 사실을 받아 알림을 보냅니다(13.4).
 *
 * <p>{@code AFTER_COMMIT} 이므로 업무 트랜잭션이 커밋된 뒤에 돕니다. 여기서 예외가 나도
 * 접수·심사·지급은 되돌아가지 않습니다. 1.2 에서 확인한 장애 전이가 여기서 끊깁니다.</p>
 *
 * <p>대신 알림이 유실될 수 있습니다. 커밋은 됐는데 알림은 안 간 상태가 생깁니다. 18.2 의
 * 아웃박스가 그 문제를 다룹니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(ApplicationEvents.Received event) {
        notificationService.notify(event.applicantId(), "email",
            "지원금 신청이 접수되었습니다",
            event.programName() + " 신청이 접수되었습니다. 신청번호 " + event.applicationId());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(ApplicationEvents.Cancelled event) {
        notificationService.notify(event.applicantId(), "email",
            "지원금 신청이 취소되었습니다",
            "신청번호 " + event.applicationId() + " 이 취소되었습니다.");
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(ApplicationEvents.Decided event) {
        if (event.approved()) {
            notificationService.notify(event.applicantId(), "sms",
                "지원금 신청이 선정되었습니다",
                "신청번호 " + event.applicationId() + " 이 선정되었습니다.");
        } else {
            notificationService.notify(event.applicantId(), "sms",
                "지원금 신청이 반려되었습니다",
                "신청번호 " + event.applicationId() + " 이 반려되었습니다. 사유: " + event.reason());
        }
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(PaymentEvents.Completed event) {
        notificationService.notify(event.applicantId(), "sms",
            "지원금이 지급되었습니다",
            "신청번호 " + event.applicationId() + " 지급이 완료되었습니다.");
    }
}
