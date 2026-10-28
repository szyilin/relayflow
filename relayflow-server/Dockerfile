# syntax=docker/dockerfile:1

# Build from the repository root:
# docker build -f relayflow-server/Dockerfile -t relayflow-server:local .
# Optional Maven settings: --secret id=maven_settings,src=/path/to/settings.xml
# Fixed multi-architecture Java 21 images; no daemon or global mirror changes needed.
ARG JDK_IMAGE=eclipse-temurin@sha256:1ff763083f2993d57d0bf374ab10bb3e2cb873af6c13a04458ebbd3e0337dc76
ARG JRE_IMAGE=eclipse-temurin@sha256:b48e02e0ea88d7bfb305335df94923bdef394905f4147dccd52e873cf17b1088
FROM ${JDK_IMAGE} AS build

WORKDIR /workspace

COPY pom.xml mvnw lombok.config ./
COPY .mvn/ .mvn/
COPY relayflow-dependencies/ relayflow-dependencies/
COPY relayflow-framework/ relayflow-framework/
COPY relayflow-module-system/ relayflow-module-system/
COPY relayflow-module-infra/ relayflow-module-infra/
COPY relayflow-module-im/ relayflow-module-im/
COPY relayflow-module-task/ relayflow-module-task/
COPY relayflow-module-calendar/ relayflow-module-calendar/
COPY relayflow-module-docs/ relayflow-module-docs/
COPY relayflow-module-rtc/ relayflow-module-rtc/
COPY relayflow-module-meeting/ relayflow-module-meeting/
COPY relayflow-module-agent/ relayflow-module-agent/
COPY relayflow-server/ relayflow-server/
COPY relayflow-tools/ relayflow-tools/

RUN --mount=type=cache,target=/root/.m2/repository \
    --mount=type=cache,target=/root/.m2/wrapper \
    --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml \
    chmod +x mvnw \
    && ./mvnw -pl relayflow-server -am package -DskipTests -B -ntp -U

FROM ${JRE_IMAGE} AS runtime

WORKDIR /app

RUN addgroup -g 10001 relayflow \
    && adduser -D -u 10001 -G relayflow relayflow \
    && mkdir -p /app/logs/xxl-job/jobhandler \
    && chown -R relayflow:relayflow /app

COPY --from=build --chown=relayflow:relayflow /workspace/relayflow-server/target/relayflow-server-*.jar /app/app.jar

USER relayflow
EXPOSE 8080 9999

HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=10 \
  CMD body="$(wget -q -T 4 -O- http://127.0.0.1:8080/admin-api/system/tenant/default)" \
    && printf '%s' "$body" | grep -Eq '"code"[[:space:]]*:[[:space:]]*0[[:space:]]*[,}]' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
