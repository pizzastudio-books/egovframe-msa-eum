#!/usr/bin/env bash
#
# 네트워크 정책이 실제로 막는지 확인하는 클러스터.
#
# 실습 클러스터(local/up.sh)의 CNI 는 kindnet 이고 **네트워크 정책을 강제하지 않습니다**.
# 정책을 걸어도 오류 없이 받아들여지고 목록에도 나오는데 아무것도 안 막힙니다(9.3).
#
# 그래서 강제하는 CNI(Calico)를 얹은 클러스터를 따로 세웁니다. 실습 클러스터는 그대로
# 두고 이것만 올렸다 내리면 됩니다.
#
#   ./up.sh          세우고 정책 전후를 잰다
#   ./down.sh        내린다
#
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
CTX=kind-netpol
CALICO_VERSION=v3.28.2

log() { printf '\033[36m▶ %s\033[0m\n' "$*"; }

if ! kind get clusters 2>/dev/null | grep -qx netpol; then
    log "클러스터를 만듭니다 (기본 CNI 없이)"
    kind create cluster --config "$HERE/kind.yaml"
fi

# 폐쇄망에서는 이 매니페스트도 반입 대상입니다(7.2). 이미지도 함께 들여와야 합니다.
log "Calico 를 올립니다 ($CALICO_VERSION)"
kubectl --context "$CTX" apply -f \
    "https://raw.githubusercontent.com/projectcalico/calico/$CALICO_VERSION/manifests/calico.yaml"
kubectl --context "$CTX" -n kube-system rollout status ds/calico-node --timeout=600s

log "이음의 세 서비스를 흉내 낸 파드를 올립니다"
kubectl --context "$CTX" apply -f "$HERE/apps.yaml"
kubectl --context "$CTX" -n eum wait --for=condition=available deploy --all --timeout=300s

probe() {
    for src in eum-notification eum-core; do
        printf '  %-18s → eum-payment: ' "$src"
        # curl 이 못 닿으면 http_code 는 000 이고 종료 코드가 붙는다. 28 은 시간 초과다.
        # 막힌 것과 거절당한 것을 이 코드로 가른다 — 거절이면 7(연결 실패)이 나온다.
        if out=$(kubectl --context "$CTX" -n eum exec deploy/"$src" -- \
                 curl -s -m 5 -o /dev/null -w '%{http_code}' http://eum-payment:8080/ 2>/dev/null); then
            echo "$out"
        else
            echo "연결 실패 (curl 종료 코드 $?)"
        fi
    done
}

# 두 번째로 돌릴 때 앞선 정책이 남아 있으면 "걸기 전"이 거짓이 된다. 먼저 지운다.
kubectl --context "$CTX" -n eum delete networkpolicy --all >/dev/null 2>&1 || true
sleep 3

echo
echo "=== 정책 걸기 전 ==="
probe

kubectl --context "$CTX" apply -f "$HERE/netpol.yaml" >/dev/null
sleep 3

echo
echo "=== 정책 건 뒤 ==="
probe

echo
echo "  알림은 막히고 본체만 닿으면 강제되는 것입니다."
echo "  같은 매니페스트를 실습 클러스터에 걸면 둘 다 200 이 나옵니다."
