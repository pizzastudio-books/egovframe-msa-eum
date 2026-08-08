package com.pizzastudio.eum.payment.domain;

/** 지급 모듈이 밖으로 알리는 사실입니다(13.4). */
public final class PaymentEvents {

    private PaymentEvents() {
    }

    /** 지급됨. */
    public record Completed(Long paymentId, String applicationId, String applicantId, Long amount) {
    }
}
