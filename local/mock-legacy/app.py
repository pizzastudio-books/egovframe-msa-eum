"""타 기관 연계 API 를 흉내 내는 서버.

13.1 에서 타 기관 데이터베이스 직결을 연계 API 호출로 바꾼다. 그때 상대편이 필요한데,
실습에서 실제 기관에 붙을 수는 없다. 이 서버가 그 자리를 대신한다.

**느리게 만들거나 실패시킬 수 있다.** 상대 기관이 느려질 때 이음이 어떻게 되는지가
13.3 서킷 브레이커의 이야기라, 그 상황을 만들 수단이 있어야 한다.

    GET  /api/legacy/business/{사업자번호}
    GET  /api/legacy/arrears/{사업자번호}
    GET  /api/legacy/echo            받은 헤더를 그대로 돌려준다(16.3 확인용)
    GET  /healthz

    POST /admin/behavior   {"delayMillis": 3000, "failRate": 0.5, "status": 500}
    GET  /admin/behavior
"""
import json
import random
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

# 실습용 가짜 자료. eum-mono 의 legacy/bizinfo.sql·taxinfo.sql 과 같은 값이다.
BUSINESS = {
    "123-45-67890": {"businessNo": "123-45-67890", "businessName": "가나다상회",
                     "openDate": "2019-03-02", "statusCode": "01"},
    "234-56-78901": {"businessNo": "234-56-78901", "businessName": "라마바식당",
                     "openDate": "2021-07-15", "statusCode": "01"},
    "345-67-89012": {"businessNo": "345-67-89012", "businessName": "사아자상사",
                     "openDate": "2015-11-30", "statusCode": "03"},
}

ARREARS = {
    "123-45-67890": {"businessNo": "123-45-67890", "arrearsAmount": 0},
    "234-56-78901": {"businessNo": "234-56-78901", "arrearsAmount": 2_500_000},
    "345-67-89012": {"businessNo": "345-67-89012", "arrearsAmount": 0},
}

behavior = {"delayMillis": 0, "failRate": 0.0, "status": 500}


class Handler(BaseHTTPRequestHandler):

    def _send(self, status, payload):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _misbehave(self):
        """설정한 대로 느려지거나 실패한다. 그런 일이 없으면 False 를 준다."""
        if behavior["delayMillis"] > 0:
            time.sleep(behavior["delayMillis"] / 1000.0)
        if behavior["failRate"] > 0 and random.random() < behavior["failRate"]:
            self._send(behavior["status"], {"message": "타 기관 시스템 오류"})
            return True
        return False

    def do_GET(self):
        path = self.path.split("?")[0]

        if path == "/healthz":
            self._send(200, {"status": "UP"})
            return

        if path == "/admin/behavior":
            self._send(200, behavior)
            return

        # 게이트웨이가 뒤로 무엇을 넘기는지 그대로 되돌려 준다(16.3).
        # 주체 헤더가 실제로 도착하는지는 받는 쪽에서 봐야 안다.
        if path == "/api/legacy/echo":
            self._send(200, {"headers": {k: v for k, v in self.headers.items()}})
            return

        if path.startswith("/api/legacy/business/"):
            if self._misbehave():
                return
            biz_no = path.rsplit("/", 1)[-1]
            found = BUSINESS.get(biz_no)
            self._send(200 if found else 404,
                       found or {"message": "사업자 등록 정보가 없습니다."})
            return

        if path.startswith("/api/legacy/arrears/"):
            if self._misbehave():
                return
            biz_no = path.rsplit("/", 1)[-1]
            # 체납 이력이 없으면 0 원이다. 없는 것과 0 원은 다르지 않다.
            self._send(200, ARREARS.get(biz_no, {"businessNo": biz_no, "arrearsAmount": 0}))
            return

        self._send(404, {"message": "그런 경로가 없습니다."})

    def do_POST(self):
        if self.path != "/admin/behavior":
            self._send(404, {"message": "그런 경로가 없습니다."})
            return
        length = int(self.headers.get("Content-Length", 0))
        given = json.loads(self.rfile.read(length) or b"{}")
        behavior.update({k: given[k] for k in ("delayMillis", "failRate", "status") if k in given})
        self._send(200, behavior)

    def log_message(self, fmt, *args):
        print(f"[mock-legacy] {fmt % args}", flush=True)


if __name__ == "__main__":
    ThreadingHTTPServer(("0.0.0.0", 9090), Handler).serve_forever()
