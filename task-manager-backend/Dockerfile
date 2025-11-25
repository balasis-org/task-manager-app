# Use JDK 21 as base
FROM eclipse-temurin:21-jdk-alpine

# Set working directory inside container
WORKDIR /app

# Copy the Spring Boot jar into the container
COPY /task-manager-context/task-manager-context-web/task-manager-context-web-domain/target/TaskManager.jar app.jar

# Expose default Spring Boot port
EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
