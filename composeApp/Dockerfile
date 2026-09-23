ARG MIRROR=docker.io
FROM ${MIRROR}/ubuntu AS builder

ARG BUILD_PROXY
ARG BUILD_PROXY_PORT

USER root

# 使用构建参数动态生成apt代理配置
RUN echo "Acquire::http::Proxy \"http://${BUILD_PROXY}:${BUILD_PROXY_PORT}\";" > /etc/apt/apt.conf.d/01proxy \
    && echo "Acquire::https::Proxy \"http://${BUILD_PROXY}:${BUILD_PROXY_PORT}\";" >> /etc/apt/apt.conf.d/01proxy

RUN apt-get update && apt-get install -y \
   wget \
   curl \
   ca-certificates \
   tar \
   binutils \
   libasound2t64 \
   libx11-6 \
   libxext6 \
   libxrender1 \
   libxtst6 \
   libxi6 \
    && rm -rf /var/lib/apt/lists/*

# 注意：一定要选 "full" 版本，它内置了 JavaFX JMODS
RUN wget -q https://download.bell-sw.com/java/25.0.2+12/bellsoft-jdk25.0.2+12-linux-amd64-full.tar.gz -O /tmp/jdk.tar.gz && \
    mkdir -p /opt/jdk && \
    tar -xzf /tmp/jdk.tar.gz -C /opt/jdk --strip-components=1 && \
    rm /tmp/jdk.tar.gz

# 3. 设置环境变量
ENV JAVA_HOME=/opt/jdk
ENV PATH="/opt/jdk/bin:${PATH}"

# 4. 暴力验证 (如果这次还报错，那才是真的见鬼了)
RUN java -version && \
    ls -l ${JAVA_HOME}/jmods/java.base.jmod && \
    ls -l ${JAVA_HOME}/jmods/javafx.graphics.jmod

# 复制依赖配置文件
WORKDIR /app

COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts version.toml ./
COPY gradle ./gradle
COPY shared/build.gradle.kts ./shared/
COPY composeApp/build.gradle.kts ./composeApp/
RUN chmod +x gradlew

# 下载依赖
RUN ./gradlew dependencies -x test --no-daemon \
    -Dhttp.proxyHost=${BUILD_PROXY} \
    -Dhttp.proxyPort=${BUILD_PROXY_PORT} \
    -Dhttps.proxyHost=${BUILD_PROXY} \
    -Dhttps.proxyPort=${BUILD_PROXY_PORT}

# 3. 复制所有源代码
COPY . .
RUN chmod -R 755 .

RUN ./gradlew composeApp:createReleaseDistributableZip -x test --no-daemon \
    -Dhttp.proxyHost=${BUILD_PROXY} \
    -Dhttp.proxyPort=${BUILD_PROXY_PORT} \
    -Dhttps.proxyHost=${BUILD_PROXY} \
    -Dhttps.proxyPort=${BUILD_PROXY_PORT}

# 专门用于导出产物的阶段
FROM scratch AS artifacts
COPY --from=builder /app/server/build/libs/server-all.jar /app.jar
COPY --from=builder /app/composeApp/build/compose/binaries/main-release/app/*.zip /release/
