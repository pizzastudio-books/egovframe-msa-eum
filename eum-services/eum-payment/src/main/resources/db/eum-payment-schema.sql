-- 지급 서비스 스키마 (데이터베이스 eum_pay, 계정 pay_app, 별도 인스턴스)
--
-- 계좌라는 민감 자료를 담고 감사 기록 보존 주기와 백업 요건이 달라, 인스턴스를
-- 나눌 근거가 실제로 있는 유일한 업무다(17.2).

CREATE TABLE IF NOT EXISTS payment (
    payment_id      BIGINT        NOT NULL AUTO_INCREMENT COMMENT '지급 id',
    event_id        VARCHAR(40)   NOT NULL COMMENT '선정 이벤트 식별자(멱등 열쇠)',
    application_id  VARCHAR(40)   NOT NULL COMMENT '신청 id',
    applicant_id    VARCHAR(50)            COMMENT '신청자 id',
    amount          BIGINT        NOT NULL COMMENT '지급 금액',
    account_no      VARCHAR(40)            COMMENT '수급 계좌 — 원본은 여기에만 있다',
    status_id       VARCHAR(30)   NOT NULL COMMENT '지급 상태',
    sent_at         DATETIME               COMMENT '이체 요청 일시',
    result_message  VARCHAR(500)           COMMENT '이체 결과',
    created_at      DATETIME      NOT NULL COMMENT '생성 일시',
    PRIMARY KEY (payment_id),
    UNIQUE KEY uk_payment_event_id (event_id)
);

CREATE TABLE IF NOT EXISTS outbox_event (
    outbox_id      BIGINT        NOT NULL AUTO_INCREMENT COMMENT '아웃박스 id',
    event_id       VARCHAR(40)   NOT NULL COMMENT '이벤트 식별자',
    event_name     VARCHAR(100)  NOT NULL COMMENT '이벤트 이름',
    aggregate_id   VARCHAR(40)            COMMENT '가리키는 업무 자료',
    payload        VARCHAR(4000) NOT NULL COMMENT '본문(JSON)',
    published_at   DATETIME               COMMENT '발행 일시',
    attempt_count  INT           NOT NULL DEFAULT 0 COMMENT '발행 시도 횟수',
    created_at     DATETIME      NOT NULL COMMENT '생성 일시',
    PRIMARY KEY (outbox_id),
    UNIQUE KEY uk_pay_outbox_event_id (event_id)
);
