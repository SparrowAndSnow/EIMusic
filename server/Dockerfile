ARG MIRROR=docker.io
FROM ${MIRROR}/gradle:9.4.0-jdk25-noble AS builder

ARG BUILD_PROXY
ARG BUILD_PROXY_PORT

WORKDIR /app

COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts version.toml ./
COPY gradle ./gradle
COPY shared/build.gradle.kts ./shared/
COPY server/build.gradle.kts ./server/

# 下载依赖
RUN gradle server:dependencies -DincludeClient=false -x test --no-daemon --info --parallel \
    -Dhttp.proxyHost=${BUILD_PROXY} \
    -Dhttp.proxyPort=${BUILD_PROXY_PORT} \
    -Dhttps.proxyHost=${BUILD_PROXY} \
    -Dhttps.proxyPort=${BUILD_PROXY_PORT}

# 3. 复制所有源代码
COPY . .

# 构建服务端
RUN gradle server:build -DincludeClient=false -x test --no-daemon --parallel \
    -Dhttp.proxyHost=${BUILD_PROXY} \
    -Dhttp.proxyPort=${BUILD_PROXY_PORT} \
    -Dhttps.proxyHost=${BUILD_PROXY} \
    -Dhttps.proxyPort=${BUILD_PROXY_PORT}

# Runner
FROM ${MIRROR}/eclipse-temurin:25.0.2_10-jre-noble

WORKDIR /app

COPY --from=builder /app/server/build/libs/server-all.jar app.jar

ENV TZ=Asia/Shanghai

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
