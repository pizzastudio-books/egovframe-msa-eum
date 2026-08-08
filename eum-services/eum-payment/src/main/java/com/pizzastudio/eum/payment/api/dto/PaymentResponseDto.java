package com.pizzastudio.eum.payment.api.dto;

import java.time.LocalDateTime;

import com.pizzastudio.eum.payment.domain.Payment;

import lombok.Getter;

/**
 * 지급 조회 응답.
 *
 * <p><b>계좌번호를 그대로 내보내지 않는다.</b> 3부의 응답은 원본을 실어 보냈다. 지급을
 * 떼어내면서 계좌 원본이 이 서비스에만 남게 되었는데, 응답이 원본을 내보내면 그 경계가
 * 무의미해진다(17.2).</p>
 */
@Getter
public class PaymentResponseDto {

    private final Long paymentId;
    private final String applicationId;
    private final String applicantId;
    private final Long amount;
    private final String accountNo;
    private final String statusId;
    private final LocalDateTime sentAt;
    private final String resultMessage;

    public PaymentResponseDto(Payment entity) {
        this.paymentId = entity.getPaymentId();
        this.applicationId = entity.getApplicationId();
        this.applicantId = entity.getApplicantId();
        this.amount = entity.getAmount();
        this.accountNo = mask(entity.getAccountNo());
        this.statusId = entity.getStatusId();
        this.sentAt = entity.getSentAt();
        this.resultMessage = entity.getResultMessage();
    }

    /** 뒷자리 넷만 남긴다. 담당자가 어느 계좌인지 알아보기에는 충분하다. */
    private static String mask(String accountNo) {
        if (accountNo == null || accountNo.length() <= 4) {
            return accountNo;
        }
        return "*".repeat(accountNo.length() - 4) + accountNo.substring(accountNo.length() - 4);
    }
}
