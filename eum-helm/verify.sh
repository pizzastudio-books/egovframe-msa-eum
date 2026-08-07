#!/usr/bin/env bash
#
# 헬름 차트가 손으로 쓴 매니페스트와 같은 결과를 내는지 확인한다.
#
# 20.1 에서 매니페스트를 차트로 옮긴다. 그때 "같은 것을 다르게 적은 것"이라고 말하려면
# 실제로 같아야 한다. 값을 하나 잘못 옮기면 차트 쪽만 조용히 달라지는데, 그 차이는
# 배포한 뒤에야 드러난다.
#
#   ./verify.sh
#
# 필요한 것: helm, kubectl, python3(+pyyaml)

set -euo pipefail
cd "$(dirname "$0")"

CHART="."
MANIFESTS="../eum-services-k8s/overlays/local"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

echo "손으로 쓴 매니페스트를 렌더합니다 ($MANIFESTS)"
kubectl kustomize "$MANIFESTS" > "$WORK/manifests.yaml"

echo "헬름 차트를 렌더합니다 (values-local.yaml)"
helm template eum "$CHART" -f values-local.yaml --namespace eum > "$WORK/helm.yaml"

python3 - "$WORK/manifests.yaml" "$WORK/helm.yaml" <<'PY'
import sys, yaml

# 브로커와 데이터베이스는 차트 밖이다. 애플리케이션 워크로드만 견준다.
INFRA = {"eum-mysql", "eum-rabbitmq", "eum-mysql-data",
         "eum-rabbitmq-data", "eum-mysql-init"}
SKIP_KINDS = {"Namespace", "StatefulSet"}


def load(path, drop_infra):
    out = {}
    for doc in yaml.safe_load_all(open(path)):
        if not doc or doc["kind"] in SKIP_KINDS:
            continue
        name = doc["metadata"]["name"]
        if drop_infra and name in INFRA:
            continue
        # 첨부 볼륨 이름만 양쪽 관례가 다르다. 견주기 위해 맞춘다.
        if name == "eum-core-attachments":
            name = "eum-core"
        out[(doc["kind"], name)] = doc
    return out


hand = load(sys.argv[1], drop_infra=True)
helm = load(sys.argv[2], drop_infra=False)

problems = []
only_hand = sorted(set(hand) - set(helm))
only_helm = sorted(set(helm) - set(hand))
if only_hand:
    problems.append(f"매니페스트에만 있는 오브젝트: {only_hand}")
if only_helm:
    problems.append(f"차트에만 있는 오브젝트: {only_helm}")

for key in sorted(set(hand) & set(helm)):
    a, b = hand[key], helm[key]
    kind, name = key

    if kind == "Deployment":
        ca = a["spec"]["template"]["spec"]["containers"][0]
        cb = b["spec"]["template"]["spec"]["containers"][0]
        for field in ("image", "resources"):
            if ca.get(field) != cb.get(field):
                problems.append(f"{kind}/{name} .{field}\n    매니페스트: {ca.get(field)}\n    차트      : {cb.get(field)}")
        if a["spec"].get("replicas") != b["spec"].get("replicas"):
            problems.append(f"{kind}/{name} .replicas: {a['spec'].get('replicas')} vs {b['spec'].get('replicas')}")
        ea = {e["name"] for e in ca.get("env", []) or []}
        eb = {e["name"] for e in cb.get("env", []) or []}
        if ea != eb:
            problems.append(f"{kind}/{name} .env: {sorted(ea)} vs {sorted(eb)}")

    if kind == "HorizontalPodAutoscaler":
        for field in ("minReplicas", "maxReplicas"):
            if a["spec"][field] != b["spec"][field]:
                problems.append(f"{kind}/{name} .{field}: {a['spec'][field]} vs {b['spec'][field]}")

    if kind == "PodDisruptionBudget":
        if a["spec"]["minAvailable"] != b["spec"]["minAvailable"]:
            problems.append(f"{kind}/{name} .minAvailable: {a['spec']['minAvailable']} vs {b['spec']['minAvailable']}")

    if kind == "ConfigMap" and a.get("data") != b.get("data"):
        problems.append(f"{kind}/{name} .data 가 다릅니다")

    if kind == "PersistentVolumeClaim":
        if a["spec"]["accessModes"] != b["spec"]["accessModes"]:
            problems.append(f"{kind}/{name} .accessModes: {a['spec']['accessModes']} vs {b['spec']['accessModes']}")
        if a["spec"]["resources"] != b["spec"]["resources"]:
            problems.append(f"{kind}/{name} .resources: {a['spec']['resources']} vs {b['spec']['resources']}")

if problems:
    print(f"\n다른 곳이 {len(problems)} 군데 있습니다.\n")
    for p in problems:
        print("  -", p)
    sys.exit(1)

print(f"\n오브젝트 {len(hand)}개가 양쪽에서 같습니다.")
PY
