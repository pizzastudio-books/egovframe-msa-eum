-- 실습용 기초 자료
--
-- H2(MySQL 모드)와 MySQL 양쪽에서 그대로 돌아야 하므로 H2 전용 MERGE 대신
-- 두 곳 다 아는 INSERT IGNORE 를 쓴다. DATE '...' 리터럴도 마찬가지 이유로 피한다.

-- 공통코드
INSERT IGNORE INTO code (code_id, code_name, parent_code_id, sort_seq, use_at) VALUES
 ('support-category',  '지원 유형',   NULL,               1, TRUE),
 ('operating',         '운영자금',    'support-category', 1, TRUE),
 ('facility',          '시설개선',    'support-category', 2, TRUE),
 ('education',         '교육훈련',    'support-category', 3, TRUE),
 ('application-status','신청 상태',   NULL,               2, TRUE),
 ('request',           '접수',        'application-status', 1, TRUE),
 ('approve',           '선정',        'application-status', 2, TRUE),
 ('reject',            '반려',        'application-status', 3, TRUE),
 ('cancel',            '취소',        'application-status', 4, TRUE),
 ('paid',              '지급완료',    'application-status', 5, TRUE),
 ('selection-means',   '선정 방법',   NULL,               3, TRUE),
 ('first-come',        '선착순',      'selection-means',  1, TRUE),
 ('evaluate',          '심사',        'selection-means',  2, TRUE),
 ('INDUSTRY',          '업종',        NULL,               4, TRUE),
 ('retail',            '소매업',      'INDUSTRY',         1, TRUE),
 ('food',              '음식점업',    'INDUSTRY',         2, TRUE),
 ('service',           '서비스업',    'INDUSTRY',         3, TRUE),
 ('manufacture',       '제조업',      'INDUSTRY',         4, TRUE),
 ('REGION',            '지역',        NULL,               5, TRUE),
 ('seoul',             '서울특별시',  'REGION',           1, TRUE),
 ('busan',             '부산광역시',  'REGION',           2, TRUE),
 ('gyeonggi',          '경기도',      'REGION',           3, TRUE);

-- 회원은 MemberDataInitializer 가 비밀번호를 인코딩해 넣는다.

-- 지원 사업
INSERT IGNORE INTO program (program_id, program_name, category_id, total_budget, remain_budget,
                    max_amount_per_case, request_start_date, request_end_date,
                    selection_means_id, purpose_content, manager_dept_name, contact_no, use_at) VALUES
 (1, '2026년 소상공인 운영자금 지원', 'operating', 500000000, 500000000, 5000000,
     '2026-01-01 00:00:00', '2026-12-31 23:59:59', 'first-come',
     '코로나19 이후 매출이 줄어든 소상공인의 운영자금을 지원합니다.', '소상공인지원과', '02-000-1111', TRUE),
 (2, '2026년 점포 시설개선 지원',     'facility',  300000000, 300000000, 3000000,
     '2026-03-01 00:00:00', '2026-06-30 23:59:59', 'evaluate',
     '노후 점포의 시설 개선 비용을 지원합니다.', '소상공인지원과', '02-000-2222', TRUE);
