USE bizinfo;
-- 가상 행정정보 (타 기관 데이터베이스)
-- 실제로는 다른 기관이 운영하고 이음은 조회 권한만 받아 직접 붙는다.

CREATE TABLE IF NOT EXISTS biz_registration (
    business_no    VARCHAR(20)  NOT NULL COMMENT '사업자등록번호',
    business_name  VARCHAR(200)          COMMENT '상호',
    open_date      DATE                  COMMENT '개업일',
    status_code    VARCHAR(2)            COMMENT '상태 01:영업 02:휴업 03:폐업',
    PRIMARY KEY (business_no)
);

INSERT IGNORE INTO biz_registration (business_no, business_name, open_date, status_code) VALUES
 ('123-45-67890', '가나다상회', '2019-03-02', '01'),
 ('234-56-78901', '라마바식당', '2021-07-15', '01'),
 ('345-67-89012', '사아자문구', '2018-01-10', '03');
