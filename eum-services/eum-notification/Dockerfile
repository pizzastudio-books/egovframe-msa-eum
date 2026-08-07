# 이음 알림 — 문자·메일 발송
#
#   ./gradlew :eum-notification:bootJar
#   docker build -t eum/eum-notification:1.0.0 eum-notification
#
# 서비스마다 Dockerfile 이 거의 같다. 기반 이미지를 올릴 때 네 곳을 다 고쳐야 하고,
# 한 곳을 빠뜨리면 그 서비스만 옛 JRE 로 돈다. 20.2 에서 파이프라인으로 묶는다.

FROM eclipse-temurin:17-jre-jammy

ENV APP_HOME=/usr/app

# 쿠버네티스 매니페스트가 runAsNonRoot 와 runAsUser: 1000 을 건다. 이미지 쪽에서도
# 그 사용자를 만들어 두어야 한다.
RUN groupadd -g 1000 eum && useradd -u 1000 -g 1000 -m -s /bin/bash eum \
    && mkdir -p ${APP_HOME} \
    && chown -R eum:eum ${APP_HOME}

WORKDIR ${APP_HOME}

# build/libs 에는 부트 jar 와 plain jar 가 함께 있다. *.jar 로 받으면 어느 쪽이
# 들어갈지 순서에 달리므로 build.gradle 에서 이름을 app.jar 로 고정해 두었다.
COPY --chown=eum:eum build/libs/app.jar app.jar

USER 1000

EXPOSE 8080

# 뒤에 붙는 인자를 자바로 넘긴다.
#
# "$@" 가 없으면 크론잡의 args 가 sh 로 들어가 그대로 사라진다. 앱은 그런 인자를 받은
# 적이 없으니 평소처럼 웹 서버로 뜨고, 잡은 영영 끝나지 않는다. 실제로 겪었다.
# 끝의 "--" 는 sh 가 첫 인자를 $0 으로 먹지 않게 자리를 채우는 것이다.
ENTRYPOINT ["sh", "-c", "exec java -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-default} -jar app.jar \"$@\"", "--"]
