package com.pizzastudio.eum.contract;

/**
 * 서비스 사이에 오가는 이벤트의 이름.
 *
 * <p>브로커의 익스체인지 이름과 짝을 이룬다. 이름을 한곳에 모아 두는 이유는,
 * 보내는 쪽과 받는 쪽이 다른 저장소에 있어도 어긋나지 않게 하기 위해서다.</p>
 */
public final class EventNames {

    /** 심사에서 선정됨 — 지급이 받는다 */
    public static final String APPLICATION_APPROVED = "application-approved";

    /** 지급 완료 — 본체가 받아 신청 상태를 갱신한다 */
    public static final String PAYMENT_COMPLETED = "payment-completed";

    /** 지급 실패 — 본체가 받아 심사 상태를 되돌린다(보상) */
    public static final String PAYMENT_FAILED = "payment-failed";

    /** 알림 발송 요청 — 알림이 받는다 */
    public static final String NOTIFICATION_REQUESTED = "notification-requested";

    private EventNames() {
    }
}
