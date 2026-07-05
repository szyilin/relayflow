# syntax=docker/dockerfile:1

# 构建耗时主要在：
#   1. 拉取基础镜像 temurin（靠 Docker registry-mirrors）
#   2. Maven 下载依赖（靠 deploy/maven/settings.xml 阿里云）
#   3. Java 编译（CPU，无法加速）
#
# 推荐：
#   cp deploy/docker-daemon.json.example → ~/.docker/daemon.json 后重启 Docker Desktop
#   DOCKER_BUILDKIT=1 docker compose -f deploy/compose.yml build server

FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

RUN apk add --no-cache bash \
    && sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories

COPY deploy/maven/settings.xml /root/.m2/settings.xml

COPY . .

RUN --mount=type=cache,target=/root/.m2/repository \
    chmod +x mvnw \
    && ./mvnw -pl relayflow-server -am package -DskipTests -B -q

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories \
    && apk add --no-cache wget

COPY --from=build /workspace/relayflow-server/target/relayflow-server-*.jar /app/app.jar

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=10 \
  CMD wget -qO- http://127.0.0.1:8080/admin-api/system/tenant/default > /dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
