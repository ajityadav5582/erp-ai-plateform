# =============================================================================
# ERP AI Platform - Multi-Stage Dockerfile
# Java 21 / Spring Boot 3.x Production Image Template
# =============================================================================

# -------------------- Build Arguments --------------------
ARG JAVA_VERSION=21
ARG APP_NAME=erp-ai-platform
ARG APP_VERSION=0.1.0-SNAPSHOT
ARG BUILD_DATE=unknown
ARG VCS_REF=unknown
ARG JAR_PATH=platform/identity/build/libs/*.jar

# -------------------- Build Stage --------------------
FROM eclipse-temurin:21-jdk-jammy AS builder

LABEL maintainer="ERP AI Platform Team" \
      org.opencontainers.image.title="${APP_NAME}" \
      org.opencontainers.image.description="ERP AI Platform Build Image" \
      org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.revision="${VCS_REF}"

WORKDIR /app

# Install minimal build dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy Gradle wrapper and build files first for optimal layer caching
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./

# Download and cache dependencies (this layer is cached unless build files change)
RUN ./gradlew dependencies --no-daemon --stacktrace

# Copy source code
COPY platform/ platform/
COPY business/ business/
COPY ai/ ai/
COPY integration/ integration/
COPY libraries/ libraries/

# Build the application JAR (skip tests for Docker build)
# This is a multi-module project; build the specific Spring Boot module.
RUN ./gradlew :platform:identity:bootJar --no-daemon -x test --stacktrace

# -------------------- Runtime Stage --------------------
FROM eclipse-temurin:21-jre-jammy AS runtime

# Re-declare build args (ARGs before the first FROM are not inherited by stages)
ARG JAR_PATH=platform/identity/build/libs/*.jar

LABEL maintainer="ERP AI Platform Team" \
      org.opencontainers.image.title="${APP_NAME}" \
      org.opencontainers.image.description="ERP AI Platform Runtime Image" \
      org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.revision="${VCS_REF}"

# Create non-root user with fixed UID/GID for Kubernetes compatibility
RUN groupadd -r -g 1001 appgroup && \
    useradd -r -u 1001 -g appgroup -m -d /home/appuser appuser

WORKDIR /app

# Install wget for health checks (minimal package)
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    && rm -rf /var/lib/apt/lists/*

# Copy JAR from builder with proper ownership
# JAR_PATH can be overridden at build time for specific services
COPY --from=builder --chown=appuser:appgroup /app/${JAR_PATH} app.jar

# Copy health check and entrypoint scripts
COPY --chown=appuser:appgroup infrastructure/docker/healthcheck.sh /app/healthcheck.sh
COPY --chown=appuser:appgroup infrastructure/docker/entrypoint.sh /app/entrypoint.sh

# Create directories for logs and temp with proper permissions
RUN mkdir -p /app/logs /app/tmp && \
    chown -R appuser:appgroup /app/logs /app/tmp && \
    chmod +x /app/healthcheck.sh /app/entrypoint.sh

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Health check - verifies application is responding
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD /app/healthcheck.sh || exit 1

# Runtime environment variables with defaults
ENV JAVA_OPTS="" \
    SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080 \
    LOGGING_FILE_NAME=/app/logs/app.log \
    MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus \
    MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED=true \
    MANAGEMENT_HEALTH_LIVENESS_STATE_ENABLED=true \
    MANAGEMENT_HEALTH_READINESS_STATE_ENABLED=true

# Entrypoint with JVM container optimizations and signal handling
ENTRYPOINT ["/app/entrypoint.sh"]
