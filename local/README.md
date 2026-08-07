# 실습 환경 (local)

노트북 한 대에 이음을 통째로 올립니다. 부록 A 의 실체입니다.

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

실습 계정은 `admin`(기관 담당자)·`user1`·`user2` 이고 비밀번호는 모두 `eum12345!` 입니다.

## 필요한 것

| | 무엇 | 왜 |
| --- | --- | --- |
| 필수 | Docker Engine 또는 Podman | 컨테이너 런타임 |
| 필수 | kind | 클러스터. 컨테이너 하나를 노드처럼 쓴다 |
| 필수 | kubectl | 클러스터를 다룬다 |
| 필수 | JDK 17 · Node 20 | 이미지에 담을 것을 빌드한다 |
| `--helm` 일 때 | Helm 3 이상 | 20.1 |

**도커 데스크톱이 아니어도 됩니다.** 기관 규모에 따라 유료라 설치 자체가 안 되는 자리가
있습니다. kind 는 도커 엔진이나 포드맨만 있으면 돕니다. 설치는 부록 A 에 있습니다.

## 클러스터 생김새

`kind-cluster.yaml` 이 정합니다.

```
eum-control-plane   인그레스 컨트롤러가 여기 뜬다. 8080·8443 을 노트북과 잇는다
eum-worker          zone-a
eum-worker2         zone-b
```

**노드를 셋 두는 이유가 있습니다.** 한 대짜리로는 파드를 여러 노드에 흩어 놓는 이야기
(12.1)나 노드를 비우는 이야기(11.4)를 재현할 수 없습니다. 워커에 붙인 `zone-a`·`zone-b`
표는 실제 관리형 클러스터의 가용 영역을 흉내 낸 것입니다.

## 타 기관 연계 서버

`mock-legacy/` 는 상대 기관이 열어 준 연계 API 를 흉내 냅니다. 13.1 에서 데이터베이스
직결을 이 API 호출로 바꿉니다.

```
GET /api/legacy/business/{사업자번호}    사업자 등록 정보
GET /api/legacy/arrears/{사업자번호}     국세 체납액
```

**느리게 만들거나 실패시킬 수 있습니다.** 잘 될 때만 확인하면 상대가 답하지 않을 때
무슨 일이 벌어지는지 모른 채 넘어갑니다. 실제 사업에서 문제가 되는 쪽은 늘 이쪽입니다.

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

본문은 관리형 클러스터를 기준으로 씁니다. 노트북에서는 이만큼 달라집니다.

| | 관리형 클러스터 | 노트북(kind) |
| --- | --- | --- |
| 벌 수 | 서비스마다 2~3벌 | 대개 1벌 |
| 자동 확장 | 서비스마다 켠다 | 본체만 켠다 |
| 첨부 볼륨 | `ReadWriteMany` | `ReadWriteOnce` (kind 기본 스토리지가 안 준다) |
| 데이터베이스 | 클러스터 밖 관리형 DB | 클러스터 안 파드 |
| 브로커 | 전용 오퍼레이터나 관리형 | 클러스터 안 파드 한 대 |
| 노드 | 여러 영역에 흩어진 여러 대 | 세 대(제어 1, 워커 2) |

**첨부 볼륨의 차이는 일부러 남겨 둔 것입니다.** 파드를 늘렸을 때 첨부 파일이 어떻게
갈리는지 10.3 에서 이 차이로 확인합니다.

데이터베이스와 브로커를 클러스터에 넣은 것은 실습 편의입니다. 운영 전제는 밖입니다 —
데이터는 파드처럼 사라지면 안 되고, 공공에서 DB 는 별도 조직이 백업과 이중화를 맡는
자산입니다(10.3).

## 인그레스 컨트롤러를 파일로 받아 둔 이유

`ingress-nginx.yaml` 은 주소에서 받아 오지 않고 저장소에 담아 두었습니다. 실습 때마다
인터넷에서 받으면 그 사이 판이 바뀌어 어제 되던 것이 오늘 안 될 수 있고, 폐쇄망에서는
아예 받아지지 않습니다. 반입 승인을 받아야 하는 것은 판을 못 박아 두어야 합니다(20.3).

안에서 쓰는 컨테이너 이미지도 폐쇄망에서는 함께 반입해야 합니다.

## 잘 안 될 때

**파드가 처음에 몇 번 재시작합니다.** 정상입니다. 데이터베이스와 브로커가 아직 안 떠서
붙을 곳이 없는 것뿐입니다. `up.sh` 가 그 순서를 기다립니다.

**`eum.local` 이 안 열립니다.** `/etc/hosts` 에 `127.0.0.1  eum.local` 이 있는지 보십시오.

**8080 이 이미 쓰이고 있습니다.** `kind-cluster.yaml` 의 `hostPort` 를 바꾸고 클러스터를
다시 만드십시오(`./down.sh --all` 뒤 `./up.sh`).

**이미지를 고쳤는데 그대로입니다.** 클러스터는 자기 안에 실린 이미지를 씁니다. `up.sh` 를
다시 돌리거나, 그 하나만 다시 실으십시오.

```bash
docker build -t eum/eum-core:1.0.0 ../eum-services/eum-core
kind load docker-image eum/eum-core:1.0.0 --name eum
kubectl rollout restart deployment/eum-core -n eum
```

**무엇이 잘못됐는지 모르겠습니다.**

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
없으므로 그 갈래를 `docker/edge.conf` 한 장이 대신합니다.
