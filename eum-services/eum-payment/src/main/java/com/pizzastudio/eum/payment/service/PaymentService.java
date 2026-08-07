package com.pizzastudio.eum.payment.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pizzastudio.eum.contract.ApplicationApprovedEvent;
import com.pizzastudio.eum.contract.EventNames;
import com.pizzastudio.eum.contract.PaymentCompletedEvent;
import com.pizzastudio.eum.contract.PaymentFailedEvent;
import com.pizzastudio.eum.payment.domain.Payment;
import com.pizzastudio.eum.payment.domain.PaymentRepository;
import com.pizzastudio.eum.payment.domain.PaymentStatus;
import com.pizzastudio.eum.payment.external.BankTransferClient;
import com.pizzastudio.eum.payment.outbox.PaymentOutbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 지급.
 *
 * <p>선정 이벤트를 받아 지급 지시를 만들고, 야간에 이체한 뒤 결과를 아웃박스에
 * 적는다. 본체는 그 결과를 받아 신청 상태를 맞추거나 되돌린다(18.3·18.4).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BankTransferClient bankTransferClient;
    private final PaymentOutbox outbox;

    /**
     * 선정 이벤트를 받아 지급 지시를 만든다.
     *
     * @return 새로 만들었으면 true, 이미 있던 이벤트면 false
     */
    public boolean createInstruction(ApplicationApprovedEvent event) {
        if (paymentRepository.existsByEventId(event.eventId())) {
            log.debug("이미 만든 지급 지시입니다. eventId={}", event.eventId());
            return false;
        }
        paymentRepository.save(Payment.builder()
            .eventId(event.eventId())
            .applicationId(event.applicationId())
            .applicantId(event.applicantId())
            .amount(event.amount())
            .accountNo(event.accountNo())
            .build());
        return true;
    }

    /**
     * 지급 대기 건을 이체한다. 결과는 아웃박스에 적는다.
     *
     * @return 지급 완료 건수
     */
    public int executeReady() {
        List<Payment> readyList =
            paymentRepository.findByStatusIdOrderByPaymentId(PaymentStatus.READY.getKey());
        int done = 0;
        for (Payment payment : readyList) {
            done += execute(payment) ? 1 : 0;
        }
        return done;
    }

    private boolean execute(Payment payment) {
        payment.markSent();
        boolean success = bankTransferClient.transfer(
            payment.getAccountNo(), payment.getAmount(), payment.getApplicationId());

        if (success) {
            payment.markDone("이체 완료");
            outbox.record(EventNames.PAYMENT_COMPLETED, payment.getApplicationId(),
                new PaymentCompletedEvent(java.util.UUID.randomUUID().toString(),
                    payment.getApplicationId(), payment.getAmount()));
            return true;
        }

        String reason = "이체 실패 — 계좌 정보를 확인하십시오";
        payment.markFailed(reason);
        // 본체가 선정을 되돌리도록 알린다(보상)
        outbox.record(EventNames.PAYMENT_FAILED, payment.getApplicationId(),
            new PaymentFailedEvent(java.util.UUID.randomUUID().toString(),
                payment.getApplicationId(), reason));
        return false;
    }

    @Transactional(readOnly = true)
    public Payment findByApplication(String applicationId) {
        return paymentRepository.findByApplicationId(applicationId).orElse(null);
    }
}
