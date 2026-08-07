#!/usr/bin/env bash
#
# 노트북에 이음을 통째로 올린다.
#
#   ./up.sh mono       3부 — 한 덩어리 배포본
#   ./up.sh services   4~5부 — 네 서비스로 나눈 배포본
#   ./up.sh services --helm    같은 것을 헬름 차트로(20.1)
#
# 하는 일 순서대로:
#   1. kind 클러스터를 만든다(없으면)
#   2. 인그레스 컨트롤러를 올린다
#   3. 이미지를 빌드해 클러스터에 실어 넣는다
#   4. 매니페스트를 올리고 준비될 때까지 기다린다
#
# 필요한 것: docker(또는 podman), kind, kubectl, JDK 17, node 20
# 헬름으로 올릴 때만 helm 이 더 필요하다.

set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
CODE="$(cd "$HERE/.." && pwd)"
CLUSTER=eum

MODE="${1:-services}"
USE_HELM="${2:-}"

log() { printf '\n\033[1m▶ %s\033[0m\n' "$*"; }
die() { printf '\n✗ %s\n' "$*" >&2; exit 1; }

command -v docker  >/dev/null || die "docker 가 없습니다. 부록 A 를 보십시오."
command -v kind    >/dev/null || die "kind 가 없습니다. 부록 A 를 보십시오."
command -v kubectl >/dev/null || die "kubectl 이 없습니다. 부록 A 를 보십시오."
docker info >/dev/null 2>&1   || die "도커 데몬에 닿지 못합니다. 도커를 먼저 켜십시오."

# 클러스터가 8080 을 노트북과 잇는다. 컴포즈가 그 자리를 이미 쓰고 있으면 클러스터가
# 만들어지지 않는데, 그 오류 문구만 보고는 원인을 알기 어렵다.
if ! kind get clusters 2>/dev/null | grep -qx "$CLUSTER"; then
    if lsof -nP -iTCP:8080 -sTCP:LISTEN >/dev/null 2>&1; then
        die "8080 을 이미 무언가 쓰고 있습니다. 컴포즈를 띄워 두었다면 먼저 내리십시오.
   cd ../eum-mono && docker compose down"
    fi
fi

# ── 1. 클러스터 ────────────────────────────────────────────────────────────
if kind get clusters 2>/dev/null | grep -qx "$CLUSTER"; then
    log "클러스터 $CLUSTER 가 이미 있습니다. 그대로 씁니다."
else
    log "클러스터를 만듭니다 (노드 3대, 2~3분 걸립니다)"
    kind create cluster --config "$HERE/kind-cluster.yaml"
fi
kubectl config use-context "kind-$CLUSTER" >/dev/null

# ── 2. 인그레스 ────────────────────────────────────────────────────────────
if kubectl get deployment -n ingress-nginx ingress-nginx-controller >/dev/null 2>&1; then
    log "인그레스 컨트롤러가 이미 있습니다."
else
    log "인그레스 컨트롤러를 올립니다"
    kubectl apply -f "$HERE/ingress-nginx.yaml"
fi
log "인그레스가 준비될 때까지 기다립니다"
kubectl wait --namespace ingress-nginx \
    --for=condition=ready pod \
    --selector=app.kubernetes.io/component=controller \
    --timeout=300s

# ── 3. 이미지 ──────────────────────────────────────────────────────────────
build_and_load() {
    local name="$1" dir="$2"
    log "이미지 $name"
    docker build -q -t "eum/$name:1.0.0" "$dir" >/dev/null
    kind load docker-image "eum/$name:1.0.0" --name "$CLUSTER" >/dev/null
}

log "화면을 빌드합니다"
build_and_load eum-web "$CODE/eum-mono/frontend"

log "타 기관 연계를 흉내 내는 서버를 빌드합니다"
build_and_load mock-legacy "$HERE/mock-legacy"

if [ "$MODE" = "mono" ]; then
    log "이음 본체를 빌드합니다 (처음에는 의존성을 받느라 몇 분 걸립니다)"
    (cd "$CODE/eum-mono" && ./gradlew bootJar -q)
    build_and_load eum-mono "$CODE/eum-mono"
else
    log "네 서비스를 빌드합니다 (처음에는 의존성을 받느라 몇 분 걸립니다)"
    (cd "$CODE/eum-services" && ./gradlew bootJar -q)
    for m in eum-core eum-notification eum-payment eum-gateway; do
        build_and_load "$m" "$CODE/eum-services/$m"
    done
fi

# ── 4. 배포 ────────────────────────────────────────────────────────────────
if [ "$MODE" = "mono" ]; then
    log "3부 배포본을 올립니다"
    kubectl apply -k "$CODE/eum-k8s/overlays/local"
    WORKLOADS="deployment/eum deployment/eum-web"
elif [ "$USE_HELM" = "--helm" ]; then
    command -v helm >/dev/null || die "helm 이 없습니다. 부록 A 를 보십시오."
    log "4~5부 배포본을 헬름으로 올립니다"
    # DB 와 브로커는 차트 밖이다. 애플리케이션과 수명이 다르다.
    kubectl apply -f "$CODE/eum-services-k8s/base/namespace.yaml"
    kubectl apply -k "$CODE/eum-services-k8s/db" -n eum
    kubectl apply -k "$CODE/eum-services-k8s/broker" -n eum
    helm upgrade --install eum "$CODE/eum-helm" -n eum -f "$CODE/eum-helm/values-local.yaml"
    WORKLOADS="deployment/eum-core deployment/eum-notification deployment/eum-payment deployment/eum-gateway deployment/eum-web"
else
    log "4~5부 배포본을 올립니다"
    kubectl apply -k "$CODE/eum-services-k8s/overlays/local"
    WORKLOADS="deployment/eum-core deployment/eum-notification deployment/eum-payment deployment/eum-gateway deployment/eum-web"
fi

log "타 기관 연계 서버를 올립니다"
kubectl apply -f "$HERE/mock-legacy/k8s.yaml"

# 데이터베이스와 브로커가 먼저 떠야 한다. 그 전까지 앱 파드는 재시작을 되풀이한다.
# 정상이다 — 붙을 곳이 아직 없을 뿐이다.
log "데이터베이스와 브로커를 기다립니다"
kubectl rollout status statefulset/eum-mysql -n eum --timeout=300s
kubectl get statefulset -n eum eum-rabbitmq >/dev/null 2>&1 \
    && kubectl rollout status statefulset/eum-rabbitmq -n eum --timeout=300s

log "애플리케이션을 기다립니다"
for w in $WORKLOADS; do
    kubectl rollout status "$w" -n eum --timeout=420s
done

# ── 안내 ───────────────────────────────────────────────────────────────────
cat <<'ANNOUNCE'

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  올라갔습니다.

  화면    http://eum.local:8080
  API     http://eum.local:8080/api/v1

  /etc/hosts 에 아래 한 줄이 있어야 합니다(한 번만 하면 됩니다).

      127.0.0.1  eum.local

  실습 계정은 admin(기관 담당자) · user1 · user2 이고 비밀번호는 모두
  eum12345! 입니다.

  파드 보기      kubectl get pods -n eum
  로그 보기      kubectl logs -n eum -l app.kubernetes.io/name=eum-core -f
  내리기         ./down.sh
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
ANNOUNCE
