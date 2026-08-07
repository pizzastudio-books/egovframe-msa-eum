USE taxinfo;
-- 가상 국세 정보 (타 기관 데이터베이스)

CREATE TABLE IF NOT EXISTS tax_arrears (
    arrears_id      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '체납 id',
    business_no     VARCHAR(20)  NOT NULL COMMENT '사업자등록번호',
    arrears_amount  BIGINT                COMMENT '체납액',
    arrears_date    DATE                  COMMENT '체납 발생일',
    PRIMARY KEY (arrears_id)
);

INSERT IGNORE INTO tax_arrears (arrears_id, business_no, arrears_amount, arrears_date) VALUES
 (1, '234-56-78901', 1500000, '2025-11-30');
