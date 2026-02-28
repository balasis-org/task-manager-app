
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgrp && adduser -S appuser -G appgrp

WORKDIR /app

COPY target/BlobCleaner.jar app.jar

RUN chown appuser:appgrp app.jar

USER appuser

ENTRYPOINT ["java", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-XX:+HeapDumpOnOutOfMemoryError", \
  "-XX:HeapDumpPath=/tmp/heapdump.hprof", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
