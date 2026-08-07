package com.pizzastudio.eum.notification.domain;

import java.time.LocalDateTime;

import com.pizzastudio.eum.common.domain.BaseEntity;

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
 * <p>템플릿에는 없던 업무다. 지금은 같은 프로세스 안에서 직접 부르기 때문에,
 * 발송이 실패하면 신청 접수까지 함께 실패한다. 4부에서 가장 먼저 떼어낸다.</p>
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "notification")
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "receiver_id", length = 50, nullable = false)
    private String receiverId;

    /** sms 또는 email */
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

    @Builder
    public Notification(Long notificationId, String receiverId, String channelId, String title,
        String content, String statusId, LocalDateTime sentAt) {
        this.notificationId = notificationId;
        this.receiverId = receiverId;
        this.channelId = channelId;
        this.title = title;
        this.content = content;
        this.statusId = statusId == null ? "ready" : statusId;
        this.sentAt = sentAt;
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
