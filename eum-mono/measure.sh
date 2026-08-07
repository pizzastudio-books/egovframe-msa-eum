#!/usr/bin/env bash
#
# 기준선 측정 — 응답 시간 · 처리량 · 배포 소요 시간.
#
#   ./measure.sh
#
# 각 부의 끝에서 같은 명령으로 다시 잰다. 손으로 재면 예열 횟수·측정 횟수·자료 건수가
# 매번 달라져 전후를 견줄 수 없다.
#
# 절대값에는 의미를 두지 않는다. 노트북에서 잰 값이라 실제 기관 서버와 다르다.
# **같은 노트북에서 같은 방식으로 다시 재는 것**이 목적이다.
#
# 필요한 것: docker(MySQL 컨테이너), JDK 17, python3, curl

set -euo pipefail
cd "$(dirname "$0")"

B=localhost:8080
OUT="${1:-measure-$(date +%Y%m%d-%H%M).txt}"

log() { printf '\n\033[1m▶ %s\033[0m\n' "$*"; }
die() { printf '\n✗ %s\n' "$*" >&2; exit 1; }

command -v docker >/dev/null || die "docker 가 없습니다."
command -v java   >/dev/null || die "JDK 가 없습니다."

# ── 준비 ───────────────────────────────────────────────────────────────────
log "MySQL 을 띄웁니다"
docker compose up -d mysql >/dev/null
until docker exec eum-mysql mysqladmin ping -uroot -proot >/dev/null 2>&1; do sleep 2; done

log "빌드합니다"
./gradlew bootJar -q

log "띄웁니다"
pkill -f "java -jar build/libs/app.jar" 2>/dev/null || true
sleep 1
SPRING_PROFILES_ACTIVE=mysql nohup java -jar build/libs/app.jar > /tmp/eum-measure.log 2>&1 &

# 상태 검사가 UP 이라고 해서 쓸 준비가 된 것은 아니다. 톰캣이 열린 뒤에도 실습 회원을
# 넣는 초기화가 남아 있어, 곧바로 로그인하면 실패한다. 실제로 겪었다.
# 그래서 상태가 아니라 **로그인이 되는지**로 판정한다. 11.1 에서 다루는 문제와 같다.
until curl -sf -o /dev/null -X POST "$B/api/v1/auth/login" \
        -H 'Content-Type: application/json' \
        -d '{"memberId":"admin","password":"eum12345!"}' 2>/dev/null; do
  sleep 0.5
done

log "측정용 자료를 넣습니다"
ADMIN=$(curl -sS -X POST "$B/api/v1/auth/login" -H 'Content-Type: application/json' \
  -d '{"memberId":"admin","password":"eum12345!"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')
TOKEN=$(curl -sS -X POST "$B/api/v1/auth/login" -H 'Content-Type: application/json' \
  -d '{"memberId":"user1","password":"eum12345!"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')
PID=$(curl -sS -X POST "$B/api/v1/programs" -H "Authorization: Bearer $ADMIN" \
  -H 'Content-Type: application/json' \
  -d '{"programName":"기준선 측정","categoryId":"operating","totalBudget":9000000000,
       "maxAmountPerCase":5000000,"requestStartDate":"2026-01-01T00:00:00",
       "requestEndDate":"2026-12-31T23:59:59"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["programId"])')

# 목록 조회에 쓸 자료. 40건이면 한 쪽(10건)을 채우고도 남는다.
for i in $(seq 1 40); do
  curl -sS -o /dev/null -X POST "$B/api/v1/applications" \
    -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
    -d "{\"programId\":$PID,\"amount\":100000,\"purposeContent\":\"기준선\",
         \"accountNo\":\"110-$i\",\"bizNo\":\"123-45-67890\"}"
done

# ── 측정 ───────────────────────────────────────────────────────────────────
{
echo "# 이음 기준선 — $(date '+%Y-%m-%d %H:%M')"
echo "# 자료: 지원사업 1건 · 신청 40건 · 프로세스 1벌"
echo

python3 - "$B" "$TOKEN" "$ADMIN" <<'PY'
"""응답 시간과 처리량.

curl 을 한 번에 한 요청씩 부르고 curl 이 스스로 잰 time_total 만 읽는다.
파이썬에서 시간을 재면 프로세스 실행 시간(5~7ms)이 섞인다.
"""
import concurrent.futures as cf, statistics, subprocess, sys, time

def once(args):
    r = subprocess.run(
        ["curl", "-sS", "-o", "/dev/null", "-w", "%{time_total} %{http_code}"] + args,
        capture_output=True, text=True)
    t, c = r.stdout.strip().split()
    return float(t) * 1000, c

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

echo
echo "## 배포 소요 시간 (5회)"
} > "$OUT"

for run in 1 2 3 4 5; do
  pkill -f "java -jar build/libs/app.jar" 2>/dev/null || true
  sleep 1
  S=$(python3 -c 'import time;print(time.time())')
  SPRING_PROFILES_ACTIVE=mysql nohup java -jar build/libs/app.jar > /tmp/eum-deploy-$run.log 2>&1 &
  until curl -sf -o /dev/null "$B/actuator/health" 2>/dev/null; do sleep 0.05; done
  E=$(python3 -c 'import time;print(time.time())')
  python3 -c "print(f'  {$run}회  {$E-$S:.1f}초')" >> "$OUT"
done

{
echo
echo "## 스프링이 보고한 기동 시간"
grep -h "Started EumApplication" /tmp/eum-deploy-*.log | grep -oE "in [0-9.]+ seconds" | sort | sed 's/^/  /'
} >> "$OUT"

pkill -f "java -jar build/libs/app.jar" 2>/dev/null || true

log "끝났습니다 — $OUT"
cat "$OUT"
