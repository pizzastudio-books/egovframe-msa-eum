# 이음 — 출발점 (eum-mono)

소상공인 지원금 신청·심사 시스템 "이음"입니다. **한 덩어리로 배포되는 상태**이고, 이 책은
여기서 출발해 컨테이너로 옮기고 쿠버네티스에 올리고 서비스로 나눕니다.

## 이 코드의 출처

업무 코드는 **전자정부 표준프레임워크 MSA 템플릿**(Apache License 2.0,
[eGovFramework/egovframe-msa-edu](https://github.com/eGovFramework/egovframe-msa-edu))의 구조를
지원금 도메인으로 옮긴 것입니다.

| 템플릿 | 이음 |
| --- | --- |
| reserve-item-service (예약 물품) | 지원사업 — 총 재고·잔여 재고가 **총 예산·잔여 예산** |
| reserve-request-service (예약 신청) | 신청 — 예약 수량이 **신청 금액** |
| reserve-check-service (예약 확인) | 심사 — 예약 승인·취소가 **선정·반려** |
| user-service | 회원 |
| portal-service (공통코드) | 공통코드 |
| 없음 | **지급**(신설) · **알림**(신설) · **야간 배치**(신설) |

템플릿의 예약 3종은 WebFlux + R2DBC였습니다. 이음은 **Web MVC + JPA**로 통일했습니다.
한 프로세스에 WebFlux와 MVC를 같이 넣을 수 없어 모놀리스 출발점을 만들 수 없기 때문입니다.
그 판단의 근거는 본문 2.4에, 리액티브 선택 자체는 부록 G에 있습니다.

## 스택

표준프레임워크 실행환경 5.0 · Spring Boot 3.5.6 · JDK 17 · JPA + QueryDSL · Gradle ·
실습은 H2(MySQL 모드), 운영 전제는 MySQL

## 돌려 보기

```
./gradlew build     # 백엔드 시험 66건
./gradlew bootRun   # http://localhost:8080

cd frontend
npm install
npm test            # 화면 시험 7건
npm run dev         # http://localhost:5173 (/api 요청은 8080 으로 프록시)
```

컨테이너로 통째로 띄우려면 이렇게 합니다(6.5).

```bash
./gradlew bootJar
docker compose up -d --build   # http://localhost:8080
```

화면과 API 가 같은 주소로 나옵니다. 컴포즈에는 인그레스가 없으므로 그 갈래를
`docker/edge.conf` 한 장이 대신합니다. 클러스터에서는 인그레스가 같은 일을 합니다(9.2).
8080 이 이미 쓰이고 있으면 `EUM_PORT=8090 docker compose up -d` 로 바꿉니다.

로그인해서 토큰을 받고 접수까지 한 바퀴 돌려 보려면 이렇게 합니다.

```bash
TOKEN=$(curl -sS -X POST localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"memberId":"user1","password":"eum12345!"}' | jq -r .token)

curl -sS -X POST localhost:8080/api/v1/applications \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"programId":1,"amount":3000000,"purposeContent":"운영자금","accountNo":"110-000-000000"}'
```

실습 계정은 `admin`(기관 담당자), `user1`·`user2`(신청자)이고 비밀번호는 모두
`eum12345!`입니다. API 문서는 <http://localhost:8080/swagger-ui.html>에 있습니다.

## 업무 흐름

```
신청자   접수 ─┬─ 신청 저장
              ├─ 지원사업 예산 차감
              └─ 알림 발송            ← 여기까지 한 트랜잭션

담당자   심사 ─┬─ 선정: 상태 변경 + 알림
              └─ 반려: 상태 변경 + 예산 복원 + 알림

배치     새벽 2시 ─┬─ 선정 건으로 지급 지시 생성
                   └─ 금융망 이체 요청 → 지급완료 + 알림
```

**접수 한 건이 신청 저장·예산 차감·알림을 한 트랜잭션으로 처리합니다.** 어느 하나가
실패하면 전부 되돌아갑니다. 지금은 이 단순함이 이점이지만, 4부에서 지급과 알림을 떼어내는
순간 성립하지 않습니다. 18장 사가가 필요해지는 자리가 여기입니다.

## 옮길 때 걸릴 것들

책에서 하나씩 다룹니다.

| 무엇 | 어디서 걸리나 | 다루는 곳 |
| --- | --- | --- |
| 인메모리 H2 | 프로세스를 여러 벌로 늘리면 각자 다른 DB를 갖는다 | 5.3 |
| `@Scheduled` 야간 배치 | 여러 벌로 늘리면 같은 배치가 중복 실행된다 | 11.5 |
| 알림이 접수와 같은 트랜잭션 | 발송이 실패하면 접수까지 실패한다 | 15장 |
| 지급이 본체 안에 있음 | 계좌라는 민감 자료가 본체 DB에 있다 | 17장 |
| 타 기관 DB 직결 | 상대 기관이 내려가면 접수도 멈춘다 | 13.1 |
| 첨부 파일이 로컬 디스크에 | 컨테이너가 죽으면 파일도 사라진다 | 5.3 · 10.3 |
| 한 트랜잭션인 접수 흐름 | 나누면 깨진다 | 18장 |

## 화면

`frontend/` 는 spring-react 에서 만든 이음 화면을 그대로 가져와 이 책의 API 규격에 맞춘
것입니다. 신청자 화면(지원사업 목록·신청·내 신청)과 기관 담당자 화면(대시보드·심사 목록),
그리고 신청 상세가 있습니다.

이 책에서 화면 개발은 다루지 않습니다. **컨테이너 한 장과 인그레스 경로로만** 나옵니다.
다만 규격 차이를 흡수하는 자리는 `frontend/src/api/` 한 곳에 모아 두었습니다.

## 구조

```
com.pizzastudio.eum
  common/        기반 — 감사 필드, 예외, 보안, 토큰
  member/        회원과 로그인
  code/          공통코드
  program/       지원사업 (공고·예산)
  application/   신청
  review/        심사
  payment/       지급
  notification/  알림
  attachment/    증빙 첨부 (서버 로컬 디스크)
  batch/         야간 지급 배치
  external/      가상 금융망 연계
    legacy/      타 기관 데이터베이스 직결
```

컴포넌트 스캔이 루트 전체라 **모듈 경계가 런타임에 존재하지 않습니다.** 지금은 어느 패키지든
서로를 부를 수 있습니다. 13장에서 이 경계를 세웁니다.

## 라이선스

표준프레임워크 MSA 템플릿의 구조를 따랐습니다. 원저작물은 Apache License 2.0이며
`NOTICE` 파일에 고지를 두었습니다.
