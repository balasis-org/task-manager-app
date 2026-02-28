
FROM eclipse-temurin:21-jre-alpine

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
