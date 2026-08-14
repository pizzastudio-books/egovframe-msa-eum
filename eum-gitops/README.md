# GitOps (20.2)

배포는 저장소를 기준으로 수행합니다. 개발자는 `kubectl`이나 `helm` 명령을 직접 입력하지 않습니다.

```bash
kubectl apply -f flux.yaml        # 컨트롤러
# 값은 Kustomization 이 저장소에서 가져온다(eum-app.yaml). 손으로 올리지 않는다.
kubectl apply -f eum-app.yaml     # 저장소와 릴리스 정의
```

## 켜야 했던 것

| 무엇 | 없으면 |
| --- | --- |
| `driftDetection: enabled` | **손으로 바꾼 값이 그대로 남는다.** 저장소와 클러스터가 조용히 벌어진다 |
| `prune: true`(Kustomization) | 저장소에서 지운 것이 클러스터에 남는다 |
| `wait` + `healthChecks` | 적용만 하고 뜨는지는 안 본다 |

첫째가 특히 조용합니다. 저장소를 고치면 시스템이 자동으로 따라오기 때문에 잘 도는 것처럼 보이지만, 누가 손으로 변경한 값은 그대로 남아 있습니다.

## 컨트롤러를 줄인 것

Flux 기본 설치는 여섯 개의 컨트롤러와 서른여덟 개의 오브젝트로 구성됩니다. 실습에서는 이 중 세 개의 컨트롤러만 사용합니다.

| 남긴 것 | 하는 일 |
| --- | --- |
| `source-controller` | git 저장소를 읽어 온다 |
| `kustomize-controller` | 매니페스트를 적용한다 |
| `helm-controller` | 헬름 차트를 릴리스로 관리한다 |

뺀 것은 `image-*`와 `notification-controller`입니다. 이 두 가지는 폐쇄망 환경에서 필요하지 않으므로 반입하지 않았습니다(20.3).

## 한 대상을 두 도구가 관리하면 안 됩니다

헬름으로 띄운 리소스를 kustomize로 다시 적용하려 하자, 컨트롤러가 두 설정 간 차이를 재는 데 시간을 소비하며 동작이 멈췄습니다.  
이에 따라 기존 헬름 릴리스를 모두 제거하고, `HelmRelease` 하나로 전체를 통합했습니다.
