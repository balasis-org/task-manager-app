
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgrp && adduser -S appuser -G appgrp

WORKDIR /app

COPY target/BlobCleaner.jar app.jar

RUN chown appuser:appgrp app.jar

USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]
