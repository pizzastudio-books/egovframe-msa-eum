package com.pizzastudio.eum.core.saga;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.pizzastudio.eum.core.application.domain.Application;
import com.pizzastudio.eum.core.application.domain.ApplicationRepository;
import com.pizzastudio.eum.core.application.domain.ApplicationStatus;
import com.pizzastudio.eum.contract.PaymentCompletedEvent;
import com.pizzastudio.eum.contract.PaymentFailedEvent;
import com.pizzastudio.eum.core.outbox.service.NotificationRequests;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 지급 결과 처리.
 *
 * <p><b>같은 이벤트가 두 번 와도 결과가 같아야 한다.</b> 브로커는 한 번 이상 전달을
 * 보장할 뿐이다. 여기서는 상태를 보고 이미 처리된 건이면 아무것도 하지 않는 방식으로
 * 멱등을 만든다. 이벤트 식별자를 따로 기록하는 방법도 있고, 그 대조는 18.4 에서 한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentResultHandler {

    private final ApplicationRepository applicationRepository;
    private final NotificationRequests notificationRequests;

    @Transactional
    public void onCompleted(PaymentCompletedEvent event) {
        Application application = find(event.applicationId());
        if (application == null || application.isPaid()) {
            log.debug("이미 처리된 지급 완료입니다. applicationId={}", event.applicationId());
            return;
        }
        application.updateStatus(ApplicationStatus.PAID.getKey());
        notificationRequests.request(application.getApplicantId(), "sms",
            "지원금이 지급되었습니다",
            "신청번호 " + application.getApplicationId() + " 지급이 완료되었습니다.");
    }

    /**
     * 보상 — 선정을 되돌린다.
     */
    @Transactional
    public void onFailed(PaymentFailedEvent event) {
        Application application = find(event.applicationId());
        if (application == null || !application.isApproved()) {
            log.debug("되돌릴 것이 없습니다. applicationId={}", event.applicationId());
            return;
        }
        application.updateStatus(ApplicationStatus.REQUEST.getKey());
        notificationRequests.request(application.getApplicantId(), "sms",
            "지급이 보류되었습니다",
            "신청번호 " + application.getApplicationId() + " 지급이 보류되었습니다. 사유: " + event.reason());
    }

    private Application find(String applicationId) {
        return applicationRepository.findById(applicationId).orElse(null);
    }
}
