package com.pizzastudio.eum.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 지급이 끝났다. */
public record PaymentCompletedEvent(
    @JsonProperty("eventId") String eventId,
    @JsonProperty("applicationId") String applicationId,
    @JsonProperty("amount") Long amount) {

    @JsonCreator
    public PaymentCompletedEvent {
    }
}
