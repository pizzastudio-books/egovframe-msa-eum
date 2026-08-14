# 이음 — 로컬·컴포즈용 이미지
#
# 기반 이미지와 구조는 6.1 에서 정한 대로다. 도커 파일이 여러 개일 때 무엇이 갈리는지는
# 6.2 에서 다룬다.
#
#   ./gradlew bootJar
#   docker build -t eum/eum-mono:1.0.0 .

FROM eclipse-temurin:17-jre-jammy

ENV APP_HOME=/usr/app
ENV EUM_FILE_DIRECTORY=${APP_HOME}/attachments

# 컨테이너를 root 로 돌리지 않는다.
#
# 쿠버네티스 매니페스트가 runAsNonRoot 와 runAsUser: 1000 을 건다. 이미지 쪽에서
# 그 사용자를 만들어 두고 폴더 소유도 넘겨야 한다. 이것을 빠뜨리면 파드는 뜨지만
# 첨부 파일을 저장할 때 권한 오류가 난다 — 배포가 끝난 뒤에야 드러나는 결함이다.
RUN groupadd -g 1000 eum && useradd -u 1000 -g 1000 -m -s /bin/bash eum \
    && mkdir -p ${EUM_FILE_DIRECTORY} \
    && chown -R eum:eum ${APP_HOME}

WORKDIR ${APP_HOME}

# 이름을 박아 넣는다.
#
# build/libs 에는 실행 가능한 부트 jar 와 라이브러리용 plain jar 가 함께 있다.
# *.jar 로 받으면 둘 다 걸려 어느 쪽이 들어갈지 순서에 달린다. plain jar 가 들어가면
# "기본 클래스를 찾을 수 없습니다" 로 죽는다. build.gradle 에서 부트 jar 이름을
# app.jar 로 고정해 두었다.
COPY --chown=eum:eum build/libs/app.jar app.jar

USER 1000

EXPOSE 8080

# 프로파일은 밖에서 넣는다. 이미지 안에 환경을 굳히지 않는다(5.2)
# 뒤에 붙는 인자를 자바로 넘긴다.
#
# "$@" 가 없으면 크론잡의 args 가 sh 로 들어가 그대로 사라진다. 앱은 그런 인자를 받은
# 적이 없으니 평소처럼 웹 서버로 뜨고, 잡은 영영 끝나지 않는다. 실제로 겪었다.
# 끝의 "--" 는 sh 가 첫 인자를 $0 으로 먹지 않게 자리를 채우는 것이다.
ENTRYPOINT ["sh", "-c", "exec java -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-default} -jar app.jar \"$@\"", "--"]
