package com.pizzastudio.eum.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 지급에 실패했다.
 *
 * <p>본체는 이 이벤트를 받아 신청을 지급보류로 되돌린다. 갈라진 흐름을 되감는
 * 보상이다(18.4).</p>
 */
public record PaymentFailedEvent(
    @JsonProperty("eventId") String eventId,
    @JsonProperty("applicationId") String applicationId,
    @JsonProperty("reason") String reason) {

    @JsonCreator
    public PaymentFailedEvent {
    }
}
