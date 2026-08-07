#!/usr/bin/env bash
#
# 올린 것을 내린다.
#
#   ./down.sh          이음만 지운다. 클러스터는 남는다(다시 올리기가 빠르다)
#   ./down.sh --all    클러스터째 지운다
#
# 클러스터를 남겨 두면 이미지도 남아 다음 실습이 훨씬 빠르다. 노트북 자원이 아쉬울 때만
# --all 을 쓰십시오.

set -euo pipefail
CLUSTER=eum

if [ "${1:-}" = "--all" ]; then
    echo "▶ 클러스터를 지웁니다"
    kind delete cluster --name "$CLUSTER"
    exit 0
fi

echo "▶ 이음을 지웁니다 (클러스터는 남깁니다)"
helm uninstall eum -n eum 2>/dev/null || true
kubectl delete namespace eum --ignore-not-found --timeout=180s
echo "끝났습니다. 다시 올리려면 ./up.sh"
