-- 이음 스키마 (단일 데이터베이스 eum, 계정 eum_app)
-- 전환 전에는 모든 업무가 이 한 곳에 모여 있다. 13장에서 소유권을 가르고
-- 15장·17장에서 데이터베이스를 나눈다.

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

CREATE TABLE IF NOT EXISTS payment (
    payment_id        BIGINT        NOT NULL AUTO_INCREMENT COMMENT '지급 id',
    application_id    VARCHAR(40)   NOT NULL COMMENT '신청 id',
    amount            BIGINT        NOT NULL COMMENT '지급 금액',
    account_no        VARCHAR(40)            COMMENT '수급 계좌',
    status_id         VARCHAR(30)   NOT NULL COMMENT '지급 상태',
    sent_at           DATETIME               COMMENT '이체 요청 일시',
    result_message    VARCHAR(500)           COMMENT '이체 결과',
    create_date       DATETIME               COMMENT '생성일',
    created_by        VARCHAR(50)            COMMENT '생성자',
    modified_date     DATETIME               COMMENT '수정일',
    last_modified_by  VARCHAR(50)            COMMENT '수정자',
    PRIMARY KEY (payment_id)
);

CREATE TABLE IF NOT EXISTS notification (
    notification_id   BIGINT        NOT NULL AUTO_INCREMENT COMMENT '발송 id',
    receiver_id       VARCHAR(50)   NOT NULL COMMENT '수신자 id',
    channel_id        VARCHAR(20)   NOT NULL COMMENT '발송 수단 sms/email',
    title             VARCHAR(200)           COMMENT '제목',
    content           VARCHAR(4000)          COMMENT '내용',
    status_id         VARCHAR(30)   NOT NULL COMMENT '발송 상태',
    sent_at           DATETIME               COMMENT '발송 일시',
    create_date       DATETIME               COMMENT '생성일',
    created_by        VARCHAR(50)            COMMENT '생성자',
    modified_date     DATETIME               COMMENT '수정일',
    last_modified_by  VARCHAR(50)            COMMENT '수정자',
    PRIMARY KEY (notification_id)
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
