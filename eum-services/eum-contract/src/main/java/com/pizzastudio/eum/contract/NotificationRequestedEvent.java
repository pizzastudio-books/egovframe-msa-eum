package com.pizzastudio.eum.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 문자·메일을 보내 달라. */
public record NotificationRequestedEvent(
    @JsonProperty("eventId") String eventId,
    @JsonProperty("receiverId") String receiverId,
    @JsonProperty("channelId") String channelId,
    @JsonProperty("title") String title,
    @JsonProperty("content") String content) {

    @JsonCreator
    public NotificationRequestedEvent {
    }
}
