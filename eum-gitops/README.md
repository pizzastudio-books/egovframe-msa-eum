# GitOps (20.2)

저장소를 기준으로 배포합니다. 손으로 `kubectl` 이나 `helm` 을 치지 않습니다.

```bash
kubectl apply -f flux.yaml        # 컨트롤러
kubectl -n eum apply -f values-cm.yaml
kubectl apply -f eum-app.yaml     # 저장소와 릴리스 정의
```

## 켜야 했던 것

| 무엇 | 없으면 |
| --- | --- |
| `driftDetection: enabled` | **손으로 바꾼 값이 그대로 남는다.** 저장소와 클러스터가 조용히 벌어진다 |
| `prune: true`(Kustomization) | 저장소에서 지운 것이 클러스터에 남는다 |
| `wait` + `healthChecks` | 적용만 하고 뜨는지는 안 본다 |

첫째가 특히 조용합니다. 저장소를 고치면 따라오니 잘 도는 것처럼 보이는데, 누가 손으로
바꾼 값은 그대로 남습니다.

## 컨트롤러를 줄인 것

Flux 기본 설치는 컨트롤러 여섯에 오브젝트 38개입니다. 실습에는 셋만 씁니다.

| 남긴 것 | 하는 일 |
| --- | --- |
| `source-controller` | git 저장소를 읽어 온다 |
| `kustomize-controller` | 매니페스트를 적용한다 |
| `helm-controller` | 헬름 차트를 릴리스로 관리한다 |

뺀 것은 `image-*`(이미지 자동 갱신)와 `notification-controller`(알림)입니다. 폐쇄망에서는
필요한 것만 반입하는 편이 낫습니다(20.3).

## 한 대상을 두 도구가 관리하면 안 됩니다

헬름으로 띄운 것을 kustomize 로 또 적용하려 하자 컨트롤러가 차이를 재느라 멈췄습니다.
그래서 헬름 릴리스를 지우고 `HelmRelease` 하나로 모았습니다.
