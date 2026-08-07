package com.pizzastudio.eum.application.domain;

import lombok.Getter;

/**
 * 신청 상태. 공통코드 application-status 와 짝을 이룬다.
 */
@Getter
public enum ApplicationStatus {

    REQUEST("request", "접수"),
    APPROVE("approve", "선정"),
    REJECT("reject", "반려"),
    CANCEL("cancel", "취소"),
    PAID("paid", "지급완료");

    private final String key;
    private final String title;

    ApplicationStatus(String key, String title) {
        this.key = key;
        this.title = title;
    }

    public boolean isEquals(String key) {
        return this.key.equals(key);
    }

    public static ApplicationStatus of(String key) {
        for (ApplicationStatus status : values()) {
            if (status.key.equals(key)) {
                return status;
            }
        }
        throw new IllegalArgumentException("알 수 없는 신청 상태입니다. " + key);
    }
}
