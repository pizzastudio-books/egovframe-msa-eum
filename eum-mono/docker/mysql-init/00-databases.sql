-- 이음 본체와 타 기관 데이터베이스를 한 인스턴스에 만든다.
-- 실제 사업에서는 기관마다 다른 서버다. 여기서는 실습 편의로 모아 둔다.

CREATE DATABASE IF NOT EXISTS eum      DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS bizinfo  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS taxinfo  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 이음 계정 하나가 세 데이터베이스를 모두 읽고 쓴다.
-- 13장에서 이 권한을 업무별로 가른다.
CREATE USER IF NOT EXISTS 'eum_app'@'%' IDENTIFIED BY 'eum_app';
GRANT ALL PRIVILEGES ON eum.*     TO 'eum_app'@'%';
GRANT SELECT           ON bizinfo.* TO 'eum_app'@'%';
GRANT SELECT           ON taxinfo.* TO 'eum_app'@'%';
FLUSH PRIVILEGES;
