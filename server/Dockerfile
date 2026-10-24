# 服务端镜像只负责运行产物，jar 由宿主 / CI 预先生成（容器内不跑 Gradle）：
#   ./gradlew :server:buildFatJar
#   docker build -f server/Dockerfile -t eimusic-backend:$(git rev-parse --short HEAD) server/build/libs
ARG MIRROR=docker.io
FROM ${MIRROR}/eclipse-temurin:25.0.2_10-jre-noble

WORKDIR /app

COPY server-all.jar app.jar

ENV TZ=Asia/Shanghai

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
