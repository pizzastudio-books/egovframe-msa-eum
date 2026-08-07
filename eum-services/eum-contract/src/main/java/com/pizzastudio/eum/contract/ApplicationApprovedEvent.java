package com.pizzastudio.eum.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 심사에서 선정됐다.
 *
 * <p>{@code eventId} 는 같은 이벤트가 두 번 와도 결과가 같도록 하는 열쇠다.
 * 브로커는 한 번 이상 전달을 보장할 뿐 정확히 한 번을 보장하지 않는다(18.4).</p>
 */
public record ApplicationApprovedEvent(
    @JsonProperty("eventId") String eventId,
    @JsonProperty("applicationId") String applicationId,
    @JsonProperty("applicantId") String applicantId,
    @JsonProperty("amount") Long amount,
    @JsonProperty("accountNo") String accountNo) {

    @JsonCreator
    public ApplicationApprovedEvent {
    }
}
