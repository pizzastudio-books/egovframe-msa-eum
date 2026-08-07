-- 알림 서비스 스키마 (데이터베이스 eum_noti, 계정 noti_app)
--
-- 본체와 물리적으로 나뉘어 있다. 계정 권한이 자기 데이터베이스로 묶여 있어
-- 알림이 신청서를 조회하려 하면 문법 오류가 아니라 권한 오류로 막힌다(15.2).

CREATE TABLE IF NOT EXISTS notification (
    notification_id  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '발송 id',
    event_id         VARCHAR(40)   NOT NULL COMMENT '이벤트 식별자(멱등 열쇠)',
    receiver_id      VARCHAR(50)   NOT NULL COMMENT '수신자 id',
    channel_id       VARCHAR(20)   NOT NULL COMMENT '발송 수단 sms/email',
    title            VARCHAR(200)           COMMENT '제목',
    content          VARCHAR(4000)          COMMENT '내용',
    status_id        VARCHAR(30)   NOT NULL COMMENT '발송 상태',
    sent_at          DATETIME               COMMENT '발송 일시',
    created_at       DATETIME      NOT NULL COMMENT '생성 일시',
    PRIMARY KEY (notification_id),
    UNIQUE KEY uk_notification_event_id (event_id)
);
