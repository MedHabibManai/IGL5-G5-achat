# Multi-stage Dockerfile for Spring Boot Application
# Stage 1: Build stage (not used in CI/CD, JAR comes from Jenkins)
# Stage 2: Runtime stage

FROM openjdk:8-jre-alpine

# Set working directory
WORKDIR /app

# Add metadata
LABEL maintainer="IGL5-G5-Achat Team"
LABEL description="Achat E-commerce Application"
LABEL version="1.0"

# Copy the JAR file from target directory
# In Jenkins, this will be the JAR built by Maven
COPY target/achat-1.0.jar app.jar

# Expose the application port
EXPOSE 8089

# Set JVM options
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8089/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

