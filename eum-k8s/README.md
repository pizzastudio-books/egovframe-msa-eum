# 이음 — 쿠버네티스 배포본 (eum-k8s)

3부에서 쓰는 스냅숏입니다. **애플리케이션은 `eum-mono` 그대로**이고 매니페스트만
늘어납니다. 아직 서비스를 나누지 않은 한 덩어리를 클러스터에 올려 운영을 견디는
상태까지 만듭니다.

## 무엇이 어디에 있나

| 파일 | 다루는 곳 |
| --- | --- |
| `base/deployment.yaml` | 파드·레플리카셋(8.2), 프로브와 무중단 종료(11.1), 자원(11.2), 롤링 업데이트(11.3), 다중 영역 배치(12.1) |
| `base/service.yaml` | 서비스와 클러스터 DNS(9.1). 별도 레지스트리를 두지 않는 근거(14.2) |
| `base/ingress.yaml` | 인그레스와 도메인·TLS(9.2), 게이트웨이와의 역할 구분(16.1) |
| `base/configmap.yaml` | 환경별 설정(10.1), 커넥션 풀(10.4) |
| `base/secret.yaml` | 비밀 관리(10.2) |
| `base/pvc.yaml` | 첨부 파일과 볼륨(10.3) |
| `base/hpa.yaml` | 부하에 따른 확장(11.5) |
| `base/pdb.yaml` | 노드 교체 중 최소 대수(12.1) |
| `base/cronjob-payment.yaml` | 야간 배치를 앱에서 떼어내기(11.5) |
| `db/mysql.yaml` | 실습용 DB. **운영 전제는 클러스터 밖**(10.3) |
| `overlays/local/` | 노트북 kind 클러스터에서 달라지는 점 |

## 돌려 보기

이미지를 먼저 만들고 kind 클러스터에 넣습니다.

```bash
# 1) 이미지 만들기
cd ../eum-mono
./gradlew build
docker build -t eum/eum-mono:1.0.0 .

# 2) kind 클러스터에 이미지 넣기 (레지스트리 없이)
kind load docker-image eum/eum-mono:1.0.0 --name eum

# 3) 배포
cd ../eum-k8s
kubectl apply -k overlays/local

# 4) 확인
kubectl -n eum rollout status deployment/eum
kubectl -n eum get pod,svc,ingress
```

클러스터 구성은 `../local/` 에 있습니다.

## 모놀리스에서 달라진 것

배치를 앱에서 뗐습니다. 모놀리스에서는 `@Scheduled` 가 앱 안에서 돌았는데, 파드를 세
벌로 늘리면 같은 배치가 세 번 돕니다. 그래서 앱 쪽 스케줄을 끄고(`eum.batch.payment.enabled=false`)
CronJob 이 같은 코드를 한 번만 부르게 했습니다. 그 과정을 11.5 에서 다룹니다.

나머지는 애플리케이션 코드를 손대지 않았습니다. **컨테이너로 옮기는 데 코드를 고칠
필요가 없다**는 것이 2부의 결론이고, 3부는 그 결론 위에서 진행합니다.
