# Use JRE 21 (no compiler/dev tools in production)
FROM eclipse-temurin:21-jre-alpine

# Non-root user for security
RUN addgroup -S appgrp && adduser -S appuser -G appgrp

# Set working directory inside container
WORKDIR /app

# Copy the Spring Boot jar into the container
COPY target/BlobCleaner.jar app.jar

# Own the jar by the app user
RUN chown appuser:appgrp app.jar

USER appuser

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
