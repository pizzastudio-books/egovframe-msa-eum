package com.pizzastudio.eum.payment.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pizzastudio.eum.application.domain.Application;
import com.pizzastudio.eum.application.domain.ApplicationRepository;
import com.pizzastudio.eum.application.domain.ApplicationStatus;
import com.pizzastudio.eum.common.exception.EntityNotFoundException;
import com.pizzastudio.eum.external.BankTransferClient;
import com.pizzastudio.eum.notification.service.NotificationService;
import com.pizzastudio.eum.payment.api.dto.PaymentResponseDto;
import com.pizzastudio.eum.payment.domain.Payment;
import com.pizzastudio.eum.payment.domain.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 지급.
 *
 * <p>선정된 신청을 모아 지급 지시를 만들고 금융망에 이체를 요청한다. 지금은 신청·심사와
 * 같은 프로세스·같은 데이터베이스에 있어 한 트랜잭션으로 처리된다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ApplicationRepository applicationRepository;
    private final BankTransferClient bankTransferClient;
    private final NotificationService notificationService;

    /**
     * 선정된 신청을 지급 지시로 만든다. 이미 만든 건은 건너뛴다.
     *
     * @return 만들어진 지급 지시 수
     */
    public int createInstructions(int limit) {
        List<Application> targets = applicationRepository.findApprovedForPayment(limit);
        int created = 0;
        for (Application application : targets) {
            if (paymentRepository.existsByApplicationId(application.getApplicationId())) {
                continue;
            }
            paymentRepository.save(Payment.builder()
                .applicationId(application.getApplicationId())
                .amount(application.getAmount())
                .accountNo(application.getAccountNo())
                .build());
            created++;
        }
        return created;
    }

    /**
     * 지급 대기 건을 이체한다.
     *
     * @return 지급 완료 건수
     */
    public int executeReady() {
        List<Payment> readyList =
            paymentRepository.findByStatusIdOrderByPaymentId(com.pizzastudio.eum.payment.domain.PaymentStatus.READY.getKey());
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

        Application application = applicationRepository.findById(payment.getApplicationId())
            .orElseThrow(() -> new EntityNotFoundException(
                "신청이 없습니다. ID=" + payment.getApplicationId()));

        if (success) {
            payment.markDone("이체 완료");
            application.updateStatus(ApplicationStatus.PAID.getKey());
            notificationService.notify(application.getApplicantId(), "sms",
                "지원금이 지급되었습니다",
                "신청번호 " + application.getApplicationId() + " 지급이 완료되었습니다.");
            return true;
        }

        payment.markFailed("이체 실패 — 계좌 정보를 확인하십시오");
        return false;
    }

    @Transactional(readOnly = true)
    public PaymentResponseDto findByApplication(String applicationId) {
        Payment payment = paymentRepository.findByApplicationId(applicationId)
            .orElseThrow(() -> new EntityNotFoundException("지급 지시가 없습니다. 신청 ID=" + applicationId));
        return PaymentResponseDto.builder().entity(payment).build();
    }
}
