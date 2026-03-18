# JRE-only Alpine image — no JDK tools in prod.
# runs as non-root (appuser). MaxRAMPercentage=75% leaves headroom
# for the container sidecar and OS overhead on Azure Container Apps.
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="balasis" \
      description="Task Manager REST API — Spring Boot 3.5 / Java 21" \
      org.opencontainers.image.source="https://github.com/balasis-org/task-manager-app"

RUN addgroup -S appgrp && adduser -S appuser -G appgrp

WORKDIR /app

COPY /context/web/domain/target/TaskManager.jar app.jar

RUN chown appuser:appgrp app.jar

EXPOSE 8080

USER appuser

ENTRYPOINT ["java", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-XX:+HeapDumpOnOutOfMemoryError", \
  "-XX:HeapDumpPath=/tmp/heapdump.hprof", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
