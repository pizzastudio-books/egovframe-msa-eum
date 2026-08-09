#!/usr/bin/env bash
#
# 브로커를 죽여 놓고 접수가 되는지 본다.
#
# 나누면서 브로커가 새 단일 장애점이 됐습니다(15.3). 아웃박스가 그것을 받아 냅니다(18.1) —
# 이벤트를 브로커에 곧바로 보내지 않고 **업무 자료와 같은 트랜잭션으로 표에 적어 두고**,
# 따로 도는 발행기가 나중에 보냅니다.
#
# 이 스크립트는 그것을 눈으로 확인합니다.
#
set -euo pipefail
B=http://localhost:8080
H=(-H "Host: eum.local")
MY() { kubectl -n eum exec eum-mysql-0 -- sh -c "mysql -uroot -p\"\$MYSQL_ROOT_PASSWORD\" -N -B -e \"$1\"" 2>/dev/null; }

T=$(curl -sS "${H[@]}" -X POST "$B/api/v1/auth/login" -H 'Content-Type: application/json' \
    -d '{"memberId":"user1","password":"eum12345!"}' | python3 -c 'import json,sys;print(json.load(sys.stdin)["token"])')
P=$(curl -sS "${H[@]}" "$B/api/v1/programs?page=0&size=1" -H "Authorization: Bearer $T" \
    | python3 -c 'import json,sys;print(json.load(sys.stdin)["content"][0]["programId"])')

post() {
    curl -sS -o /dev/null -w '%{http_code}' "${H[@]}" -X POST "$B/api/v1/applications" \
        -H "Authorization: Bearer $T" -H 'Content-Type: application/json' \
        -d "{\"programId\":$P,\"amount\":100000,\"purposeContent\":\"브로커 정지 훈련\",\"accountNo\":\"110-$1\",\"bizNo\":\"123-45-67890\"}"
}
state() {
    printf '  아직 못 보낸 이벤트 %s건 · 배달된 알림 %s건\n' \
        "$(MY 'select count(*) from eum.outbox_event where published_at is null;')" \
        "$(MY 'select count(*) from eum_noti.notification;')"
}

echo "=== 시작 ==="; state

echo
echo "=== 브로커를 내립니다 ==="
kubectl -n eum scale statefulset eum-rabbitmq --replicas=0 >/dev/null
until [ -z "$(kubectl -n eum get pod -l app.kubernetes.io/name=eum-rabbitmq --no-headers 2>/dev/null)" ]; do sleep 2; done
echo "  브로커 파드 0"

for i in 1 2 3; do echo "  접수 $i → $(post "outage-$i")"; done
sleep 8
state

echo
echo "=== 브로커를 되살립니다 ==="
kubectl -n eum scale statefulset eum-rabbitmq --replicas=1 >/dev/null
kubectl -n eum rollout status statefulset/eum-rabbitmq --timeout=300s >/dev/null
sleep 25
state
echo
echo "  접수는 브로커가 없어도 201 이고, 알림은 브로커가 살아난 뒤 따라옵니다."
