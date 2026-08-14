# 실습 환경 (local)

이음을 노트북 한 대에 통째로 올립니다. 이는 부록 A의 실체입니다.

```bash
./up.sh services      # 4~5부 — 네 서비스로 나눈 배포본
./up.sh mono          # 3부 — 한 덩어리 배포본
./up.sh services --helm   # 같은 것을 헬름 차트로(20.1)

./down.sh             # 이음만 내린다 (클러스터는 남는다)
./down.sh --all       # 클러스터째 지운다
```

다 올라가면 <http://eum.local:8080> 입니다. `/etc/hosts` 에 한 줄이 필요합니다.

```
127.0.0.1  eum.local
```

실습 계정은 `admin`(기관 담당자), `user1`, `user2`이며, 비밀번호는 모두 `eum12345!`입니다.

## 필요한 것

| | 무엇 | 왜 |
| --- | --- | --- |
| 필수 | Docker Engine 또는 Podman | 컨테이너 런타임 |
| 필수 | kind | 클러스터. 컨테이너 하나를 노드처럼 쓴다 |
| 필수 | kubectl | 클러스터를 다룬다 |
| 필수 | JDK 17 · Node 20 | 이미지에 담을 것을 빌드한다 |
| `--helm` 일 때 | 헬름 3 이상 | 20.1 |

**도커 데스크톱이 아니어도 됩니다.** 기관 규모에 따라 도커 데스크톱이 유료라 설치가 불가능한 경우가 있습니다. kind는 도커 엔진이나 포드맨만 설치되어 있으면 사용할 수 있습니다. kind 설치 방법은 부록 A에 정리했습니다.

## 클러스터 생김새

`kind-cluster.yaml` 파일에 클러스터의 구성이 정해져 있습니다.

```
eum-control-plane   인그레스 컨트롤러가 여기 뜬다. 8080·8443 을 노트북과 잇는다
eum-worker          zone-a
eum-worker2         zone-b
```

**노드를 셋 두는 이유가 있습니다.** 한 대짜리 클러스터에서는 파드를 여러 노드에 분산하여 배치하는 작업(12.1)이나 노드를 비우는 작업(11.4)을 재현할 수 없습니다. 워커 노드에 붙인 `zone-a`·`zone-b` 라벨은 실제 관리형 클러스터의 가용 영역을 모방한 것입니다.

## 타 기관 연계 서버

`mock-legacy/` 디렉터리는 상대 기관이 제공한 연계 API를 모방합니다. 13.1에서는 데이터베이스에 직접 접근하던 로직을 이 API 호출로 대체합니다.

```
GET /api/legacy/business/{사업자번호}    사업자 등록 정보
GET /api/legacy/arrears/{사업자번호}     국세 체납액
```

**느리게 만들거나 실패시킬 수 있습니다.** 정상적으로 동작할 때만 테스트하면, 상대 서비스가 응답하지 않을 경우 어떤 문제가 발생하는지 파악하지 못한 채 넘어가기 쉽습니다. 실제 사업 운영에서 문제로 이어지는 경우는 대부분 이와 같은 상황입니다.

```bash
# 3초 늦게 답하게 한다 (이음의 읽기 제한은 2초다)
kubectl exec -n eum deploy/mock-legacy -- python -c "
import urllib.request, json
urllib.request.urlopen(urllib.request.Request(
    'http://localhost:9090/admin/behavior',
    data=json.dumps({'delayMillis': 3000}).encode(), method='POST'))"

# 절반을 실패시킨다
... data=json.dumps({'delayMillis': 0, 'failRate': 0.5}).encode() ...

# 되돌린다
... data=json.dumps({'delayMillis': 0, 'failRate': 0.0}).encode() ...
```

13.1 의 전환은 값 하나로 합니다. 업무 코드는 손대지 않습니다.

```bash
kubectl set env deployment/eum-core -n eum EUM_LEGACY_MODE=api
```

## 관리형 클러스터와 다른 것

본문은 관리형 클러스터를 기준으로 작성합니다. 노트북 환경에서는 이와 같은 설정이 일부 달라집니다.

| | 관리형 클러스터 | 노트북(kind) |
| --- | --- | --- |
| 레플리카 수 | 서비스마다 2~3벌 | 대개 1벌 |
| 자동 확장 | 서비스마다 켠다 | 본체만 켠다 |
| 첨부 볼륨 | `ReadWriteMany` | `ReadWriteOnce` (kind 기본 스토리지가 안 준다) |
| 데이터베이스 | 클러스터 밖 관리형 DB | 클러스터 안 파드 |
| 브로커 | 전용 오퍼레이터나 관리형 | 클러스터 안 파드 한 대 |
| 노드 | 여러 영역에 흩어진 여러 대 | 세 대(제어 1, 워커 2) |

**첨부 볼륨의 차이는 일부러 남겨 둔 것입니다.** 파드를 늘릴 때 첨부 파일이 어떻게 분산되는지 확인하기 위해, 10.3에서 이 차이를 활용합니다.

데이터베이스와 브로커를 클러스터에 넣은 것은 실습 편의입니다. 운영 전제는 밖입니다 —
데이터는 파드처럼 사라지면 안 되고, 공공에서 DB 는 별도 조직이 백업과 이중화를 맡는
자산입니다(10.3).

## 인그레스 컨트롤러를 파일로 받아 둔 이유

`ingress-nginx.yaml` 파일은 인터넷에서 직접 받아 오지 않고, 저장소에 미리 담아 두었습니다. 실습을 할 때마다 인터넷에서 매번 다운로드하면, 중간에 내용이 바뀔 수 있어 어제는 정상적으로 동작하던 설정이 오늘은 작동하지 않을 수 있습니다. 또한 폐쇄망 환경에서는 인터넷 접근 자체가 불가능하므로, 파일을 받아 오는 방식은 사용할 수 없습니다. 반입 승인을 받아야 하는 경우라면, 버전을 고정해 놓는 것이 안정적인 운영을 위해 필요합니다(20.3).

컨테이너 이미지도 폐쇄망 내에서 사용하려면, 함께 반입해야 합니다.

## 잘 안 될 때

**파드가 처음에 몇 차례 재시작합니다.** 이는 정상적인 동작입니다. 데이터베이스와 브로커가 아직 실행되지 않아 연결할 수 있는 대상이 없기 때문입니다. `up.sh` 스크립트는 이 순서를 기다린 후 다음 단계로 진행합니다.

**`eum.local` 이 안 열립니다.** `/etc/hosts` 파일에 `127.0.0.1  eum.local` 이 등록되어 있는지 확인하십시오.

**8080 포트가 이미 사용 중입니다.** `kind-cluster.yaml` 파일의 `hostPort` 값을 변경한 뒤, 클러스터를 다시 생성하십시오. 먼저 `./down.sh --all` 명령을 실행하여 기존 클러스터를 제거하고, 그다음 `./up.sh` 명령으로 새 클러스터를 띄우십시오.

**이미지를 고쳤는데 그대로입니다.** 클러스터는 내부에 실린 이미지를 사용합니다. `up.sh` 를 다시 실행하거나, 해당 이미지만 다시 실으십시오.

```bash
docker build -t eum/eum-core:1.0.0 ../eum-services/eum-core
kind load docker-image eum/eum-core:1.0.0 --name eum
kubectl rollout restart deployment/eum-core -n eum
```

**어떤 부분에서 문제가 발생했는지 확인되지 않습니다.**

```bash
kubectl get pods -n eum                                   # 상태
kubectl describe pod -n eum <파드이름>                     # 이벤트 — 프로브·스케줄 실패가 여기 있다
kubectl logs -n eum <파드이름> --previous                  # 재시작 전 로그
```

## 클러스터 없이 돌려 보기

쿠버네티스까지 가기 전(1~2부)에는 컴포즈로 충분합니다.

```bash
cd ../eum-mono
./gradlew bootJar
docker compose up -d --build
```

<http://localhost:8080> 입니다. 화면과 API 가 같은 주소로 나옵니다 — 컴포즈에는 인그레스가
해당 갈래가 없으므로 `docker/edge.conf` 파일 하나가 그 역할을 대신합니다.

## 도커에 메모리를 넉넉히 주십시오

**8GB 이상을 권합니다.** 실제 운영에서 3.8GB로 설정했을 때 네 서비스를 동시에 띄우는 과정에서 API 서버가 응답을 멈췄고, 오류 메시지로 `TLS handshake timeout`이 발생했습니다. 이어 coredns 파드가 종료되어 도메인 이름 풀이 기능이 작동하지 않았으며, 다른 파드들도 재시작을 반복했습니다. 이 현상은 코드나 매니페스트에 문제가 있는 것처럼 보였지만, 근본 원인은 노드의 자원 부족이었습니다.

실측 결과입니다(2026-08-08 기준, 세 노드 합계).

| 무엇 | 메모리 |
| --- | --- |
| eum-control-plane | 722MiB |
| eum-worker | 1.17GiB |
| eum-worker2 | 1.16GiB |
| **합계** | **약 3.0GiB** |

여기에 이미지 빌드가 겹치면 넘칩니다. 도커 데스크톱은 설정 → Resources 에서 늘립니다.

**증상으로 구별하는 법**입니다.

| 증상 | 자원 부족일 때 |
| --- | --- |
| `TLS handshake timeout` | API 서버가 CPU 를 못 얻는다 |
| coredns `CrashLoopBackOff` | 위와 같은 원인 |
| 파드가 `UnknownHostException` | coredns 가 죽어서다. 앱 문제가 아니다 |
| 종료 코드 137 | 메모리가 모자라 죽었다 |
