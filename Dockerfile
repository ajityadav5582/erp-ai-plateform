# ERP AI Platform - Multi-stage Dockerfile
# Optimized for production deployment

# Build stage
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Copy Gradle files
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./

# Download dependencies
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY platform/ platform/
COPY business/ business/
COPY ai/ ai/
COPY integration/ integration/
COPY libraries/ libraries/

# Build application
RUN ./gradlew bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

# Create non-root user
RUN groupadd -r erpai && useradd -r -g erpai erpai

# Copy JAR from builder
COPY --from=builder --chown=erpai:erpai /app/platform/core/interfaces/build/libs/*.jar app.jar

# Switch to non-root user
USER erpai

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health/readiness || exit 1

# Run application
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/tmp/heapdump.hprof", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dmanagement.endpoints.web.exposure.include=health,info,metrics,prometheus", \
    "-jar", "app.jar"]
