#!/usr/bin/env bash
#
# 기준선 측정 — 클러스터판.
#
# eum-mono/measure.sh 와 **재는 방법이 같습니다.** 예열 20회, 측정 150회, curl 이 스스로 잰
# time_total 만 읽습니다. 처리량은 동시 1·5·10·20·40 로 각 10초입니다. 방법이 같아야
# 3부와 5부를 견줄 수 있습니다(4.2·21.3).
#
# **1·2부 값과는 그대로 견주지 마십시오.** 그쪽은 노트북에서 프로세스 하나로 쟀고 여기는
# 클러스터에서 파드 여럿입니다. 인그레스 한 단이 더 있고 요청이 노드를 건너갑니다.
# 견줄 수 있는 것은 클러스터끼리 — 3부(한 덩어리)와 5부(나눈 뒤)입니다.
#
# 쓰는 법:
#   ./local/up.sh mono  && ./measure-k8s.sh 3부
#   ./local/up.sh       && ./measure-k8s.sh 5부
#
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"

LABEL="${1:-현재}"
B=http://localhost:8080
H=(-H "Host: eum.local")
OUT="$HERE/measure-k8s-$LABEL.txt"

log() { printf '\033[36m▶ %s\033[0m\n' "$*"; }

# 배포 단위 이름이 부마다 다릅니다. 3부는 한 덩어리(eum-app), 5부는 넷입니다.
if kubectl get deploy eum -n eum >/dev/null 2>&1; then
    DEPLOYS=(eum)
    FRONT=eum              # 배포 중 실패를 잴 때 다시 굴릴 것
else
    DEPLOYS=(eum-gateway eum-core eum-notification eum-payment)
    FRONT=eum-core         # 접수를 처리하는 것이 본체다
fi

log "대상: ${DEPLOYS[*]}"

# 상태가 UP 이라고 쓸 준비가 된 것은 아닙니다. 로그인이 되는지로 판정합니다(4.2 와 같은 이유).
log "로그인이 될 때까지 기다립니다"
until curl -sf -o /dev/null -X POST "${H[@]}" "$B/api/v1/auth/login" \
        -H 'Content-Type: application/json' \
        -d '{"memberId":"user1","password":"eum12345!"}' 2>/dev/null; do
    sleep 1
done

TOKEN=$(curl -sS "${H[@]}" -X POST "$B/api/v1/auth/login" -H 'Content-Type: application/json' \
    -d '{"memberId":"user1","password":"eum12345!"}' | python3 -c 'import json,sys;print(json.load(sys.stdin)["token"])')
ADMIN=$(curl -sS "${H[@]}" -X POST "$B/api/v1/auth/login" -H 'Content-Type: application/json' \
    -d '{"memberId":"admin","password":"eum12345!"}' | python3 -c 'import json,sys;print(json.load(sys.stdin)["token"])')

# 자료 건수를 남긴다. **건수가 다르면 응답 시간을 견줄 수 없다.** 목록 조회는 자료가
# 쌓일수록 느려지므로, 조건을 적어 두지 않은 측정값은 나중에 쓸 수 없다.
ROWS=$(kubectl exec -n eum eum-mysql-0 -- sh -c \
    'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -B -e "select count(*) from eum.application; select count(*) from eum.program;"' \
    2>/dev/null | tr '\n' ' ')

{
echo "# 기준선 — $LABEL (클러스터)"
echo
echo "  자료: 신청·지원사업 = $ROWS"
echo
kubectl get deploy -n eum -o custom-columns=이름:.metadata.name,레플리카:.spec.replicas --no-headers | sed 's/^/  /'
echo
} > "$OUT"

log "응답 시간과 처리량"
python3 - "$B" "$TOKEN" "$ADMIN" <<'PY' >> "$OUT"
import concurrent.futures as cf, statistics, subprocess, sys, time

HOST = ["-H", "Host: eum.local"]

def once(args):
    r = subprocess.run(
        ["curl", "-sS", "-o", "/dev/null", "-w", "%{time_total} %{http_code}"] + HOST + args,
        capture_output=True, text=True)
    out = r.stdout.strip().split()
    if len(out) != 2:
        return 0.0, "000"
    return float(out[0]) * 1000, out[1]

def latency(name, args, n=150, warm=20):
    for _ in range(warm):
        once(args)
    times, bad = [], 0
    for _ in range(n):
        ms, code = once(args)
        times.append(ms)
        bad += code not in ("200", "201")
    times.sort()
    return dict(name=name, n=n, bad=bad, avg=statistics.mean(times),
                p50=times[len(times)//2], p95=times[int(len(times)*0.95)],
                p99=times[int(len(times)*0.99)], mx=times[-1])

def throughput(args, workers, seconds=10):
    stop = time.time() + seconds
    times, bad = [], 0

    def worker():
        out = []
        while time.time() < stop:
            out.append(once(args))
        return out

    with cf.ThreadPoolExecutor(workers) as ex:
        for fut in [ex.submit(worker) for _ in range(workers)]:
            for ms, code in fut.result():
                times.append(ms)
                bad += code not in ("200", "201")
    times.sort()
    return dict(workers=workers, total=len(times), bad=bad, tps=len(times)/seconds,
                p50=times[len(times)//2], p95=times[int(len(times)*0.95)],
                p99=times[int(len(times)*0.99)])

B, TOKEN, ADMIN = sys.argv[1], sys.argv[2], sys.argv[3]
U = ["-H", f"Authorization: Bearer {TOKEN}"]
A = ["-H", f"Authorization: Bearer {ADMIN}"]

print("## 응답 시간")
print(f"{'요청':<14}{'건수':>5}{'실패':>5}{'평균':>10}{'p50':>9}{'p95':>9}{'p99':>9}{'최대':>9}")
for name, args in [
    ("로그인",       ["-X","POST",f"{B}/api/v1/auth/login","-H","Content-Type: application/json",
                      "-d",'{"memberId":"user1","password":"eum12345!"}']),
    ("지원사업 목록", [f"{B}/api/v1/programs?page=0&size=10"] + U),
    ("내 신청 목록",  [f"{B}/api/v1/applications/mine?page=0&size=10"] + U),
    ("신청 통계",     [f"{B}/api/v1/applications/stats"] + A),
    ("상태 검사",     [f"{B}/actuator/health"]),
]:
    r = latency(name, args)
    print(f"{r['name']:<14}{r['n']:>5}{r['bad']:>5}"
          f"{r['avg']:>9.1f}ms{r['p50']:>7.1f}ms{r['p95']:>7.1f}ms{r['p99']:>7.1f}ms{r['mx']:>7.1f}ms")

print()
print("## 처리량 (내 신청 목록, 동시 요청 수별 10초)")
print(f"{'동시':>5}{'총 건수':>9}{'실패':>6}{'초당':>9}{'p50':>9}{'p95':>9}{'p99':>9}")
load_args = [f"{B}/api/v1/applications/mine?page=0&size=10"] + U
for w in (1, 5, 10, 20, 40):
    r = throughput(load_args, w)
    print(f"{r['workers']:>5}{r['total']:>9}{r['bad']:>6}{r['tps']:>8.1f}건"
          f"{r['p50']:>7.1f}ms{r['p95']:>7.1f}ms{r['p99']:>7.1f}ms")
PY

# 배포 소요 — 다시 굴리기를 걸고 전부 준비될 때까지.
log "배포 소요 (3회)"
{ echo; echo "## 배포 소요 시간 (rollout restart → 전부 Ready, 3회)"; } >> "$OUT"
for run in 1 2 3; do
    S=$(python3 -c 'import time;print(time.time())')
    for d in "${DEPLOYS[@]}"; do kubectl rollout restart deploy/"$d" -n eum >/dev/null; done
    for d in "${DEPLOYS[@]}"; do kubectl rollout status deploy/"$d" -n eum --timeout=600s >/dev/null; done
    E=$(python3 -c 'import time;print(time.time())')
    python3 -c "print(f'  {$run}회  {$E-$S:.1f}초')" >> "$OUT"
done

# 배포 중 실패 — 4.2 와 같은 방식. 0.2초 간격으로 접수를 넣으면서 다시 굴린다.
log "배포 중 실패 요청"
{ echo; echo "## 배포 중 실패 요청 (접수를 0.2초 간격으로 넣으면서 다시 굴리기)"; } >> "$OUT"
PID=$(curl -sS "${H[@]}" "$B/api/v1/programs?page=0&size=1" -H "Authorization: Bearer $TOKEN" \
    | python3 -c 'import json,sys;d=json.load(sys.stdin);print(d["content"][0]["programId"])')

(
    kubectl rollout restart deploy/"$FRONT" -n eum >/dev/null
    kubectl rollout status deploy/"$FRONT" -n eum --timeout=600s >/dev/null
    touch /tmp/eum-k8s-rollout-done
) &
rm -f /tmp/eum-k8s-rollout-done
total=0; fail=0
while [ ! -f /tmp/eum-k8s-rollout-done ]; do
    code=$(curl -sS -o /dev/null -w '%{http_code}' --max-time 10 "${H[@]}" \
        -X POST "$B/api/v1/applications" -H "Authorization: Bearer $TOKEN" \
        -H 'Content-Type: application/json' \
        -d "{\"programId\":$PID,\"amount\":100000,\"purposeContent\":\"기준선\",\"accountNo\":\"110-0\",\"bizNo\":\"123-45-67890\"}" \
        2>/dev/null || echo 000)
    total=$((total+1))
    case "$code" in 201|200) ;; *) fail=$((fail+1)); echo "    $total 번째 → $code" >> "$OUT" ;; esac
    sleep 0.2
done
wait
rm -f /tmp/eum-k8s-rollout-done
echo "  총 $total 건 중 실패 $fail 건" >> "$OUT"

log "끝났습니다 — $OUT"
cat "$OUT"
