package com.pizzastudio.eum.payment.domain;

import lombok.Getter;

@Getter
public enum PaymentStatus {

    READY("ready", "지급대기"),
    SENT("sent", "이체요청"),
    DONE("done", "지급완료"),
    FAILED("failed", "지급실패");

    private final String key;
    private final String title;

    PaymentStatus(String key, String title) {
        this.key = key;
        this.title = title;
    }

    public boolean isEquals(String key) {
        return this.key.equals(key);
    }
}
