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

데이터베이스와 브로커는 차트에 포함되어 있지 않습니다. 대신 `../eum-services-k8s/db/` 디렉터리와 `broker/` 디렉터리에서 각각 별도로 배포합니다.  
이는 데이터베이스와 브로커가 클러스터 외부에서 운영되는 것을 전제로 하기 때문입니다. 따라서 애플리케이션의 배포 주기와 수명이 서로 다릅니다.  
차트를 삭제하더라도 데이터베이스가 함께 삭제되어서는 안 됩니다.

## 같은 결과를 내는지 확인합니다

```bash
./verify.sh
```

두 방식을 각각 렌더한 뒤, 오브젝트 단위로 비교합니다. 값 하나를 잘못 옮기면 차트 쪽만 조용히 달라지고, 그 차이는 배포 후에야 확인됩니다.

```
손으로 쓴 매니페스트를 렌더합니다 (../eum-services-k8s/overlays/local)
헬름 차트를 렌더합니다 (values-local.yaml)

오브젝트 25개가 양쪽에서 같습니다.
```

## 값 파일

- `values.yaml` — 관리형 클러스터 기준
- `values-local.yaml` — 노트북 kind 클러스터. 레플리카 수와 자원 요청을 줄인다

기관마다 다른 값은 값 파일로 갈립니다. 차트는 그대로 두고 값만 바꿔 개발계·검증계·운영계에
같은 것을 올립니다. 매니페스트를 환경마다 복사해 두면 어느 것이 최신인지 아무도 모르게
됩니다.

## 서비스 목록이 곧 설정입니다

`values.yaml` 파일에서 `services` 섹션 아래에 정의된 내용이 전부입니다.

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

이 항목을 추가하면 배포, 서비스, 컨피그맵, HPA, PDB가 모두 함께 생성됩니다.

## 알아 둘 것

**설정만 바꿔도 파드가 다시 뜹니다.** 템플릿은 컨피그맵 내용의 해시를 파드의 표기에 기록합니다. 이 기능이 없으면 컨피그맵을 수정해도 실행 중인 파드는 기존 값을 계속 사용합니다. 배포는 성공했지만 설정 값이 반영되지 않는 상황이 여기서 발생합니다.

**자동 확장을 켠 서비스에는 `replicas` 를 명시하지 않습니다.** 배포와 HPA가 둘 다 레플리카 수를 정하려 하면, 서로의 설정을 덮어씁니다.

**이미지 태그에 `latest`를 사용하지 않습니다.** 태그를 명시하지 않으면 차트의 `appVersion` 값이 기본으로 적용됩니다. 어느 버전의 이미지가 실행 중인지 파악할 수 없으면, 장애 발생 시 해당 버전으로 되돌릴 수 없습니다.

**비밀은 실습용입니다.** `values.yaml` 파일의 `secrets` 항목은 저장소에 직접 두면 안 되는 값입니다.  
운영 환경에서는 기관의 비밀 관리 체계를 사용하거나, 클러스터 외부의 비밀 연동 기능을 통해 값을 주입합니다(10.2).
