# 타 기관 연계 API 를 흉내 내는 서버.
#
# 파이썬 표준 라이브러리만 쓴다. 실습 환경에 설치할 것을 늘리지 않으려는 것이고,
# 폐쇄망에서 반입할 것도 그만큼 줄어든다(20.3).
FROM python:3.12-alpine

WORKDIR /app
COPY app.py .

RUN adduser -D -u 1000 mock
USER 1000

EXPOSE 9090
CMD ["python", "-u", "app.py"]
