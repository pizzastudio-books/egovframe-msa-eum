package com.pizzastudio.eum.payment.domain;

import java.time.LocalDateTime;

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
 * <p>{@code eventId} 를 함께 적어 같은 선정 이벤트가 두 번 와도 지시를 두 번 만들지
 * 않는다(18.4). 계좌 원본은 이 표에만 있다 — 본체는 뒷자리만 들고 있다(13.1).</p>
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "event_id", length = 40, nullable = false, unique = true)
    private String eventId;

    @Column(name = "application_id", length = 40, nullable = false)
    private String applicationId;

    @Column(name = "applicant_id", length = 50)
    private String applicantId;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "account_no", length = 40)
    private String accountNo;

    @Column(name = "status_id", length = 30, nullable = false)
    private String statusId;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "result_message", length = 500)
    private String resultMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public Payment(String eventId, String applicationId, String applicantId, Long amount,
        String accountNo) {
        this.eventId = eventId;
        this.applicationId = applicationId;
        this.applicantId = applicantId;
        this.amount = amount;
        this.accountNo = accountNo;
        this.statusId = PaymentStatus.READY.getKey();
        this.createdAt = LocalDateTime.now();
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

    public boolean isReady() {
        return PaymentStatus.READY.isEquals(this.statusId);
    }
}
