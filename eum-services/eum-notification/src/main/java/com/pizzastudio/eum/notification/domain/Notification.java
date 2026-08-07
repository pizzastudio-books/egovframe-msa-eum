package com.pizzastudio.eum.notification.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 발송 요청 한 건.
 *
 * <p>{@code eventId} 를 함께 적는다. 같은 이벤트가 두 번 와도 두 번 보내지 않기
 * 위해서다(18.4).</p>
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "event_id", length = 40, nullable = false, unique = true)
    private String eventId;

    @Column(name = "receiver_id", length = 50, nullable = false)
    private String receiverId;

    @Column(name = "channel_id", length = 20, nullable = false)
    private String channelId;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "content", length = 4000)
    private String content;

    @Column(name = "status_id", length = 30, nullable = false)
    private String statusId;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public Notification(String eventId, String receiverId, String channelId, String title,
        String content) {
        this.eventId = eventId;
        this.receiverId = receiverId;
        this.channelId = channelId;
        this.title = title;
        this.content = content;
        this.statusId = "ready";
        this.createdAt = LocalDateTime.now();
    }

    public Notification markSent() {
        this.statusId = "sent";
        this.sentAt = LocalDateTime.now();
        return this;
    }

    public Notification markFailed() {
        this.statusId = "failed";
        return this;
    }
}
