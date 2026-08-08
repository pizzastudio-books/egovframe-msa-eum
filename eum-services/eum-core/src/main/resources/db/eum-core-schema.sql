-- 이음 본체 스키마 (데이터베이스 eum, 계정 eum_app)
--
-- 지급과 알림은 각자의 데이터베이스로 떨어져 나갔다. 여기에는 그 표가 없다.

CREATE TABLE IF NOT EXISTS code (
    code_id           VARCHAR(30)   NOT NULL COMMENT '코드 id',
    code_name         VARCHAR(200)  NOT NULL COMMENT '코드 명',
    parent_code_id    VARCHAR(30)            COMMENT '상위 코드 id',
    sort_seq          INT                    COMMENT '정렬 순서',
    use_at            BOOLEAN       DEFAULT TRUE COMMENT '사용 여부',
    create_date       DATETIME               COMMENT '생성일',
    created_by        VARCHAR(50)            COMMENT '생성자',
    modified_date     DATETIME               COMMENT '수정일',
    last_modified_by  VARCHAR(50)            COMMENT '수정자',
    PRIMARY KEY (code_id)
);

CREATE TABLE IF NOT EXISTS member (
    member_id         VARCHAR(50)   NOT NULL COMMENT '회원 id',
    password          VARCHAR(200)  NOT NULL COMMENT '비밀번호',
    member_name       VARCHAR(100)  NOT NULL COMMENT '이름 또는 상호',
    business_no       VARCHAR(20)            COMMENT '사업자등록번호',
    contact_no        VARCHAR(30)            COMMENT '연락처',
    email_addr        VARCHAR(200)           COMMENT '이메일',
    role_id           VARCHAR(20)   NOT NULL COMMENT '권한',
    use_at            BOOLEAN       DEFAULT TRUE COMMENT '사용 여부',
    create_date       DATETIME               COMMENT '생성일',
    created_by        VARCHAR(50)            COMMENT '생성자',
    modified_date     DATETIME               COMMENT '수정일',
    last_modified_by  VARCHAR(50)            COMMENT '수정자',
    PRIMARY KEY (member_id)
);

CREATE TABLE IF NOT EXISTS program (
    program_id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '지원 사업 id',
    program_name         VARCHAR(200)  NOT NULL COMMENT '사업 명',
    category_id          VARCHAR(30)   NOT NULL COMMENT '지원 유형 - 공통코드 support-category',
    total_budget         BIGINT        NOT NULL COMMENT '총 예산',
    remain_budget        BIGINT                 COMMENT '잔여 예산',
    max_amount_per_case  BIGINT                 COMMENT '건당 최대 신청 금액',
    operation_start_date DATETIME               COMMENT '사업 시작일',
    operation_end_date   DATETIME               COMMENT '사업 종료일',
    request_start_date   DATETIME      NOT NULL COMMENT '접수 시작일',
    request_end_date     DATETIME      NOT NULL COMMENT '접수 종료일',
    selection_means_id   VARCHAR(30)            COMMENT '선정 방법',
    purpose_content      VARCHAR(4000)          COMMENT '사업 목적',
    manager_dept_name    VARCHAR(200)           COMMENT '담당 부서',
    contact_no           VARCHAR(30)            COMMENT '문의처',
    use_at               BOOLEAN       DEFAULT TRUE COMMENT '사용 여부',
    create_date          DATETIME               COMMENT '생성일',
    created_by           VARCHAR(50)            COMMENT '생성자',
    modified_date        DATETIME               COMMENT '수정일',
    last_modified_by     VARCHAR(50)            COMMENT '수정자',
    PRIMARY KEY (program_id)
);

CREATE TABLE IF NOT EXISTS application (
    application_id        VARCHAR(40)   NOT NULL COMMENT '신청 id',
    program_id            BIGINT        NOT NULL COMMENT '지원 사업 id',
    applicant_id          VARCHAR(50)   NOT NULL COMMENT '신청자 id',
    amount                BIGINT        NOT NULL COMMENT '신청 금액',
    purpose_content       VARCHAR(4000)          COMMENT '신청 사유',
    attachment_code       VARCHAR(50)            COMMENT '증빙 첨부 코드',
    status_id             VARCHAR(30)            COMMENT '신청 상태 - 공통코드 application-status',
    reject_reason         VARCHAR(4000)          COMMENT '반려 사유',
    biz_no                VARCHAR(20)            COMMENT '사업자등록번호',
    biz_name              VARCHAR(200)           COMMENT '상호',
    owner_name            VARCHAR(100)           COMMENT '대표자명',
    industry_code         VARCHAR(30)            COMMENT '업종 - 공통코드 INDUSTRY',
    region_code           VARCHAR(30)            COMMENT '사업장 소재지 - 공통코드 REGION',
    applicant_contact_no  VARCHAR(30)            COMMENT '신청자 연락처',
    applicant_email_addr  VARCHAR(200)           COMMENT '신청자 이메일',
    account_no            VARCHAR(40)            COMMENT '수급 계좌',
    create_date           DATETIME               COMMENT '생성일',
    created_by            VARCHAR(50)            COMMENT '생성자',
    modified_date         DATETIME               COMMENT '수정일',
    last_modified_by      VARCHAR(50)            COMMENT '수정자',
    PRIMARY KEY (application_id),
    CONSTRAINT fk_application_program FOREIGN KEY (program_id) REFERENCES program (program_id)
);

CREATE TABLE IF NOT EXISTS review (
    review_id         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '심사 id',
    application_id    VARCHAR(40)   NOT NULL COMMENT '신청 id',
    reviewer_id       VARCHAR(50)   NOT NULL COMMENT '심사자 id',
    result_id         VARCHAR(30)   NOT NULL COMMENT '심사 결과 approve/reject',
    opinion           VARCHAR(4000)          COMMENT '심사 의견',
    reviewed_at       DATETIME               COMMENT '심사 일시',
    create_date       DATETIME               COMMENT '생성일',
    created_by        VARCHAR(50)            COMMENT '생성자',
    modified_date     DATETIME               COMMENT '수정일',
    last_modified_by  VARCHAR(50)            COMMENT '수정자',
    PRIMARY KEY (review_id)
);



CREATE TABLE IF NOT EXISTS attachment (
    attachment_id     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '첨부 id',
    application_id    VARCHAR(40)   NOT NULL COMMENT '신청 id',
    original_name     VARCHAR(300)  NOT NULL COMMENT '올린 파일 이름',
    stored_name       VARCHAR(300)  NOT NULL COMMENT '저장된 파일 이름',
    content_type      VARCHAR(200)           COMMENT '파일 종류',
    file_size         BIGINT                 COMMENT '크기(바이트)',
    create_date       DATETIME               COMMENT '생성일',
    created_by        VARCHAR(50)            COMMENT '생성자',
    modified_date     DATETIME               COMMENT '수정일',
    last_modified_by  VARCHAR(50)            COMMENT '수정자',
    PRIMARY KEY (attachment_id)
);

-- 아직 브로커로 보내지 않은 이벤트(18.2).
-- 업무 자료와 같은 트랜잭션으로 저장해, 저장은 됐는데 발행 전에 죽는 틈을 막는다.
CREATE TABLE IF NOT EXISTS outbox_event (
    outbox_id      BIGINT        NOT NULL AUTO_INCREMENT COMMENT '아웃박스 id',
    event_id       VARCHAR(40)   NOT NULL COMMENT '이벤트 식별자(멱등 열쇠)',
    event_name     VARCHAR(100)  NOT NULL COMMENT '이벤트 이름',
    aggregate_id   VARCHAR(40)            COMMENT '가리키는 업무 자료',
    payload        VARCHAR(4000) NOT NULL COMMENT '본문(JSON)',
    request_id     VARCHAR(64)            COMMENT '이 이벤트를 만든 요청의 식별자(19.1)',
    published_at   DATETIME               COMMENT '발행 일시. NULL 이면 아직 안 보냄',
    attempt_count  INT           NOT NULL DEFAULT 0 COMMENT '발행 시도 횟수',
    created_at     DATETIME      NOT NULL COMMENT '생성 일시',
    PRIMARY KEY (outbox_id),
    UNIQUE KEY uk_outbox_event_id (event_id)
);
