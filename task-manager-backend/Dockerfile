
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="balasis" \
      description="Task Manager REST API — Spring Boot 3.5 / Java 21" \
      org.opencontainers.image.source="https://github.com/balasis-org/task-manager-app"

RUN addgroup -S appgrp && adduser -S appuser -G appgrp

WORKDIR /app

COPY /task-manager-context/task-manager-context-web/task-manager-context-web-domain/target/TaskManager.jar app.jar

RUN chown appuser:appgrp app.jar

EXPOSE 8080

USER appuser

ENTRYPOINT ["java", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-XX:+HeapDumpOnOutOfMemoryError", \
  "-XX:HeapDumpPath=/tmp/heapdump.hprof", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
