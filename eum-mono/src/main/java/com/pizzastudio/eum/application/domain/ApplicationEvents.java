package com.pizzastudio.eum.application.domain;

/**
 * 신청 모듈이 밖으로 알리는 사실입니다(13.4).
 *
 * <p>모듈이 다른 모듈의 서비스를 직접 부르면 그 모듈을 떼어낼 때 코드를 고쳐야 합니다.
 * 사실만 알리고 누가 무엇을 할지는 받는 쪽이 정하게 두면, 떼어낼 때 받는 쪽만 바꾸면 됩니다.</p>
 */
public final class ApplicationEvents {

    private ApplicationEvents() {
    }

    /** 접수됨. */
    public record Received(String applicationId, String applicantId, String programName) {
    }

    /** 취소됨. */
    public record Cancelled(String applicationId, String applicantId) {
    }

    /** 심사 결과가 정해짐. */
    public record Decided(String applicationId, String applicantId, boolean approved, String reason) {
    }
}
