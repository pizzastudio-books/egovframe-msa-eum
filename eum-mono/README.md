# 이음 — 출발점 (eum-mono)

소상공인 지원금 신청·심사 시스템 "이음"입니다. **한 덩어리로 배포되는 상태**이고, 이 책은
여기서 출발해 컨테이너로 옮기고 쿠버네티스에 올리고 서비스로 나눕니다.

## 이 코드의 출처

업무 코드는 **전자정부 표준프레임워크 MSA 템플릿**(Apache License 2.0,  
[eGovFramework/egovframe-msa-edu](https://github.com/eGovFramework/egovframe-msa-edu)) 구조를 기반으로 하여,  
지원금 도메인에 맞게 재구성한 것입니다.

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

표준프레임워크 실행환경 5.0, Spring Boot 3.5.16, JDK 17, JPA와 QueryDSL, Gradle을 사용합니다.  
실습 환경으로는 H2 데이터베이스를 MySQL 모드로 구성하고, 운영 환경 전제는 MySQL입니다.

## 돌려 보기

```
./gradlew build     # 백엔드 시험 66건
./gradlew bootRun   # http://localhost:8080

cd frontend
npm install
npm test            # 화면 시험 7건
npm run dev         # http://localhost:5173 (/api 요청은 8080 으로 프록시)
```

컨테이너로 통째로 띄우는 방법은 6.5절에 나와 있습니다.

```bash
./gradlew bootJar
docker compose up -d --build   # http://localhost:8080
```

화면과 API가 같은 주소에서 제공됩니다. 도커 컴포즈 환경에서는 인그레스가 없으므로, 이 역할을 `docker/edge.conf` 파일 하나가 대신합니다. 쿠버네티스 클러스터에서는 인그레스가 같은 기능을 수행합니다(9.2). 포트 8080이 이미 사용 중인 경우, `EUM_PORT=8090 docker compose up -d` 명령으로 포트를 변경하여 실행합니다.

로그인하여 토큰을 발급받고, 접수까지 일련의 과정을 수행하려면 다음과 같이 합니다.

```bash
TOKEN=$(curl -sS -X POST localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"memberId":"user1","password":"eum12345!"}' | jq -r .token)

curl -sS -X POST localhost:8080/api/v1/applications \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"programId":1,"amount":3000000,"purposeContent":"운영자금","accountNo":"110-000-000000"}'
```

실습 계정은 기관 담당자를 위한 `admin`, 신청자를 위한 `user1`과 `user2`입니다. 모든 계정의 비밀번호는 `eum12345!`입니다. API 문서는 <http://localhost:8080/swagger-ui.html>에서 확인할 수 있습니다.

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

**접수 한 건은 신청 저장, 예산 차감, 알림 전송을 하나의 트랜잭션으로 처리합니다.**  
이 중 어느 하나라도 실패하면 전체 작업이 롤백됩니다.  
현재는 이 방식이 단순해 장점으로 작용하지만, 4부에서 지급과 알림을 별도로 분리하는 순간 이 방식은 더 이상 유지되지 않습니다.  
이 지점에서 18장에서 설명하는 사가 패턴이 필요해집니다.

## 옮길 때 걸릴 것들

책에서 하나씩 다룹니다.

| 무엇 | 어디서 걸리나 | 다루는 곳 |
| --- | --- | --- |
| 인메모리 H2 | 프로세스를 여러 개로 늘리면 각자 다른 DB를 갖는다 | 5.3 |
| `@Scheduled` 야간 배치 | 여러 개로 늘리면 같은 배치가 중복 실행된다 | 11.5 |
| 알림이 접수와 같은 트랜잭션 | 발송이 실패하면 접수까지 실패한다 | 15장 |
| 지급이 본체 안에 있음 | 계좌라는 민감 자료가 본체 DB에 있다 | 17장 |
| 타 기관 DB 직결 | 상대 기관이 내려가면 접수도 멈춘다 | 13.1 |
| 첨부 파일이 로컬 디스크에 | 컨테이너가 죽으면 파일도 사라진다 | 5.3 · 10.3 |
| 한 트랜잭션인 접수 흐름 | 나누면 깨진다 | 18장 |

## 화면

`frontend/` 디렉터리는 spring-react로 개발한 이음 화면을 그대로 반입하여, 이 책에서 정의한 API 규격에 맞게 수정한 것입니다. 신청자 화면(지원사업 목록, 신청, 내 신청), 기관 담당자 화면(대시보드, 심사 목록), 그리고 신청 상세 화면이 포함됩니다.

이 책에서는 화면 개발을 다루지 않습니다. **컨테이너 한 장과 인그레스 경로로만** 구현합니다.  
다만 규격 차이를 흡수하는 로직은 `frontend/src/api/` 디렉터리에 모두 모아 두었습니다.

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

컴포넌트 스캔 범위가 루트 전체로 설정되어 있어, **모듈 경계가 런타임에 적용되지 않습니다.**  
현재는 패키지 간 호출 제약이 없어, 어느 패키지든 다른 패키지를 자유롭게 호출할 수 있습니다.  
이 경계는 13장에서 명시적으로 정의합니다.

## 라이선스

표준프레임워크 MSA 템플릿의 구조를 따랐습니다. 원저작물은 Apache License 2.0으로 배포되며, `NOTICE` 파일에 저작권 고지를 기재했습니다.
