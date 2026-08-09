#!/usr/bin/env bash
#
# 폐쇄망에 들여와야 하는 이미지 목록을 뽑는다(20.3).
#
# 7장에서 애플리케이션 이미지 반입을 다뤘다. 그때는 우리가 만든 것이었다.
# **관측과 GitOps 를 들이면 남이 만든 이미지가 늘어난다.** 그것도 반입 승인 대상이다.
#
# 매니페스트에서 뽑으므로 손으로 관리하지 않는다. 구성이 바뀌면 목록도 바뀐다.
#
#   ./airgap-images.sh              목록만
#   ./airgap-images.sh --save 경로   내려받아 파일로 저장(반출용)

set -euo pipefail
cd "$(dirname "$0")/.."

MANIFESTS=(
  eum-gitops/flux.yaml
  eum-observability/prometheus.yaml
  eum-observability/grafana.yaml
  eum-services-k8s/db/mysql.yaml
  eum-services-k8s/broker/rabbitmq.yaml
  eum-services-k8s/db/init-job.yaml
  eum-services-k8s/db/migrate-job.yaml
)

images() {
  for f in "${MANIFESTS[@]}"; do
    [ -f "$f" ] || continue
    python3 - "$f" <<'PY'
import sys, yaml
for d in yaml.safe_load_all(open(sys.argv[1])):
    if not d:
        continue
    kind = d.get("kind")
    spec = None
    if kind in ("Deployment", "StatefulSet", "DaemonSet"):
        spec = d["spec"]["template"]["spec"]
    elif kind == "Job":
        spec = d["spec"]["template"]["spec"]
    elif kind == "CronJob":
        spec = d["spec"]["jobTemplate"]["spec"]["template"]["spec"]
    if not spec:
        continue
    for c in spec.get("initContainers", []) + spec.get("containers", []):
        print(c["image"])
PY
  done | sort -u
}

if [ "${1:-}" = "--save" ]; then
  OUT="${2:?저장할 경로를 주십시오}"
  mkdir -p "$OUT"
  images | while read -r img; do
    name="$(echo "$img" | tr '/:' '__').tar"
    echo "  $img"
    docker pull -q "$img" >/dev/null
    docker save "$img" -o "$OUT/$name"
    # 7.2 에서 한 대로 해시를 함께 남긴다. 옮긴 뒤 대조할 근거다.
    shasum -a 256 "$OUT/$name" >> "$OUT/SHA256SUMS"
  done
  echo "저장했습니다: $OUT"
else
  echo "▶ 폐쇄망에 들여와야 하는 이미지 (우리가 만든 것 제외)"
  images | sed 's/^/  /'
  echo
  echo "  이 목록은 매니페스트에서 뽑은 것입니다. 구성이 바뀌면 다시 뽑으십시오."
  echo "  반출: ./airgap-images.sh --save ../반출본"
fi
