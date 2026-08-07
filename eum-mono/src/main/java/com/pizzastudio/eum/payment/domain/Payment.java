package com.pizzastudio.eum.payment.domain;

import java.time.LocalDateTime;

import com.pizzastudio.eum.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지급 지시 한 건.
 *
 * <p>템플릿에는 없던 업무다. 야간 배치가 선정된 신청을 모아 지급 지시를 만들고
 * 가상 금융망으로 이체를 요청한다. 4부에서 이 업무를 통째로 떼어낸다.</p>
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "payment")
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "application_id", length = 40, nullable = false)
    private String applicationId;

    @Column(name = "amount", nullable = false)
    private Long amount;

    /** 수급 계좌 */
    @Column(name = "account_no", length = 40)
    private String accountNo;

    @Column(name = "status_id", length = 30, nullable = false)
    private String statusId;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "result_message", length = 500)
    private String resultMessage;

    @Builder
    public Payment(Long paymentId, String applicationId, Long amount, String accountNo,
        String statusId, LocalDateTime sentAt, String resultMessage) {
        this.paymentId = paymentId;
        this.applicationId = applicationId;
        this.amount = amount;
        this.accountNo = accountNo;
        this.statusId = statusId == null ? PaymentStatus.READY.getKey() : statusId;
        this.sentAt = sentAt;
        this.resultMessage = resultMessage;
    }

    public Payment markSent() {
        this.statusId = PaymentStatus.SENT.getKey();
        this.sentAt = LocalDateTime.now();
        return this;
    }

    public Payment markDone(String message) {
        this.statusId = PaymentStatus.DONE.getKey();
        this.resultMessage = message;
        return this;
    }

    public Payment markFailed(String message) {
        this.statusId = PaymentStatus.FAILED.getKey();
        this.resultMessage = message;
        return this;
    }

    public boolean isDone() {
        return PaymentStatus.DONE.isEquals(this.statusId);
    }
}
