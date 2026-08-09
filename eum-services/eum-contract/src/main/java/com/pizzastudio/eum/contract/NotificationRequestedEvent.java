package com.pizzastudio.eum.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 문자·메일을 보내 달라.
 *
 * <p><b>빠진 값은 스스로 막는다.</b> 형식이 어긋난 이벤트를 넣어 봤더니 아무것도 안 막았다 —
 * {@code content} 없이 보낸 것이 그대로 처리되어 <b>본문이 비어 있는 알림이 "발송 완료"로
 * 기록됐다</b>. 로그에도 {@code content=null} 로만 남았다(15.2).</p>
 *
 * <p>보내는 쪽과 받는 쪽이 다른 저장소에 있으므로, 한쪽이 형식을 바꾸면 다른 쪽은 다음
 * 배포 때까지 모른다. <b>그 틈을 여기서 막는다.</b> 여기서 던진 예외는 재시도를 거쳐
 * 죽은편지 큐로 간다(15.4) — 빈 알림을 보내는 것보다 낫다.</p>
 *
 * <p>모르는 필드는 일부러 안 막는다. 보내는 쪽이 값을 하나 더 붙여도 받는 쪽이 안 깨져야
 * 서로 다른 판이 함께 돌 수 있다.</p>
 */
public record NotificationRequestedEvent(
    @JsonProperty("eventId") String eventId,
    @JsonProperty("receiverId") String receiverId,
    @JsonProperty("channelId") String channelId,
    @JsonProperty("title") String title,
    @JsonProperty("content") String content) {

    @JsonCreator
    public NotificationRequestedEvent {
        require(eventId, "eventId");
        require(receiverId, "receiverId");
        require(channelId, "channelId");
        require(title, "title");
        require(content, "content");
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                "알림 이벤트에 " + name + " 이(가) 없습니다. 보내는 쪽 판을 확인하십시오.");
        }
    }
}
