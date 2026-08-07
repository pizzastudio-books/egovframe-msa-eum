package com.pizzastudio.eum.payment.api.dto;

import java.time.LocalDateTime;

import com.pizzastudio.eum.payment.domain.Payment;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentResponseDto {

    private Long paymentId;
    private String applicationId;
    private Long amount;
    private String accountNo;
    private String statusId;
    private LocalDateTime sentAt;
    private String resultMessage;

    @Builder
    public PaymentResponseDto(Payment entity) {
        this.paymentId = entity.getPaymentId();
        this.applicationId = entity.getApplicationId();
        this.amount = entity.getAmount();
        this.accountNo = entity.getAccountNo();
        this.statusId = entity.getStatusId();
        this.sentAt = entity.getSentAt();
        this.resultMessage = entity.getResultMessage();
    }
}
