# Use JRE 21 (no compiler/dev tools in production)
FROM eclipse-temurin:21-jre-alpine

# Non-root user for security
RUN addgroup -S appgrp && adduser -S appuser -G appgrp

# Set working directory inside container
WORKDIR /app

# Copy the Spring Boot jar into the container
COPY /task-manager-context/task-manager-context-web/task-manager-context-web-domain/target/TaskManager.jar app.jar

# Own the jar by the app user
RUN chown appuser:appgrp app.jar

# Expose default Spring Boot port
EXPOSE 8080

USER appuser

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
