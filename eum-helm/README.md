# 이음 헬름 차트 (eum-helm)

20.1 에서 쓰는 차트입니다. `../eum-services-k8s/` 의 매니페스트와 **같은 것을 냅니다.**
적는 방식만 다릅니다.

| | 손으로 쓴 매니페스트 | 이 차트 |
| --- | --- | --- |
| 파일 | 31장 | 템플릿 9장 + 거들기 1장 + 값 파일 2장 |
| 서비스를 하나 더 나눌 때 | 다섯 장을 새로 만든다 | `values.yaml` 에 항목 하나를 더한다 |
| 자원 요청을 다 같이 바꿀 때 | 다섯 곳을 고친다 | 한 곳을 고친다 |

## 돌려 보기

```bash
helm lint .
helm template eum . -f values-local.yaml --namespace eum   # 눈으로 확인

helm install eum . -n eum --create-namespace -f values-local.yaml
helm upgrade eum . -n eum -f values-local.yaml
helm rollback eum 1 -n eum        # 되돌리기
helm history eum -n eum
```

DB 와 브로커는 차트에 없습니다. `../eum-services-k8s/db/` 와 `broker/` 로 따로 올립니다.
운영 전제가 클러스터 밖이라 애플리케이션 배포와 수명이 다르기 때문입니다. 차트를
지웠다고 데이터베이스가 같이 지워지면 안 됩니다.

## 같은 결과를 내는지 확인합니다

```bash
./verify.sh
```

두 방식을 각각 렌더해 오브젝트 단위로 견줍니다. 값 하나를 잘못 옮기면 차트 쪽만 조용히
달라지는데, 그 차이는 배포한 뒤에야 드러납니다.

```
손으로 쓴 매니페스트를 렌더합니다 (../eum-services-k8s/overlays/local)
헬름 차트를 렌더합니다 (values-local.yaml)

오브젝트 25개가 양쪽에서 같습니다.
```

## 값 파일

- `values.yaml` — 관리형 클러스터 기준
- `values-local.yaml` — 노트북 kind 클러스터. 벌 수와 자원 요청을 줄인다

기관마다 다른 값은 값 파일로 갈립니다. 차트는 그대로 두고 값만 바꿔 개발계·검증계·운영계에
같은 것을 올립니다. 매니페스트를 환경마다 복사해 두면 어느 것이 최신인지 아무도 모르게
됩니다.

## 서비스 목록이 곧 설정입니다

`values.yaml` 의 `services` 아래가 전부입니다.

```yaml
services:
  core:
    image:
      repository: eum/eum-core
    port: 8080
    database: eum          # 있으면 DB 계정 환경변수를 넣는다
    messaging: true        # 있으면 브로커 계정 환경변수를 넣는다
    storage:               # 있으면 PVC 를 만들고 붙인다
      enabled: true
    autoscaling:
      enabled: true
    cronJob:               # 있으면 크론잡을 만든다
      enabled: false
```

여기에 항목을 더하면 배포·서비스·컨피그맵·HPA·PDB 가 함께 생깁니다.

## 알아 둘 것

**설정만 바꿔도 파드가 다시 뜹니다.** 템플릿이 컨피그맵 내용의 해시를 파드 표기에 적어
둡니다. 이것이 없으면 컨피그맵을 고쳐도 도는 파드는 옛 값을 그대로 들고 있습니다. 배포는
성공했는데 값이 안 먹는 상황이 여기서 나옵니다.

**자동 확장을 켠 서비스에는 `replicas` 를 내보내지 않습니다.** 배포와 HPA 가 둘 다 벌
수를 정하려 들면 서로 값을 되돌립니다.

**이미지 태그에 `latest` 를 쓰지 않습니다.** 태그를 적지 않으면 차트의 `appVersion` 을
씁니다. 어느 판이 돌고 있는지 알 수 없으면 장애가 났을 때 되돌릴 곳도 알 수 없습니다.

**비밀은 실습용입니다.** `values.yaml` 의 `secrets` 는 저장소에 두면 안 되는 값입니다.
운영에서는 기관 비밀 관리 체계나 클러스터의 외부 비밀 연동으로 넣습니다(10.2).
