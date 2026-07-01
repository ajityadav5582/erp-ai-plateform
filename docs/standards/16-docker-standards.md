# Docker Standards

## 1. Purpose

These standards ensure consistent, secure, and efficient containerization across the ERP SaaS platform. All Dockerfiles and container configurations must follow these guidelines.

## 2. General Principles

- **Minimal images:** Use smallest possible base images
- **Security:** Run as non-root, scan for vulnerabilities
- **Layer caching:** Optimize Dockerfile layer ordering
- **Reproducibility:** Pin versions, use digests
- **Multi-stage builds:** Separate build and runtime environments
- **Java 21:** All services use Java 21 with Spring Boot 3.x

## 3. Base Image Standards

### 3.1 Approved Base Images

| Image | Use Case | Tag Pattern |
|-------|----------|-------------|
| `eclipse-temurin:21-jdk-jammy` | Java build | `21-jdk-jammy` |
| `eclipse-temurin:21-jre-jammy` | Java runtime | `21-jre-jammy` |
| `eclipse-temurin:21-jdk-alpine` | Java build ( Alpine ) | `21-jdk-alpine` |
| `eclipse-temurin:21-jre-alpine` | Java runtime ( Alpine ) | `21-jre-alpine` |
| `nginx:alpine` | Nginx proxy | `alpine` |
| `prom/prometheus` | Prometheus | `latest` |
| `grafana/grafana` | Grafana | `latest` |

### 3.2 Image Tagging

- **Production:** Use specific version tags with digest (e.g., `21-jre-jammy@sha256:...`)
- **Staging:** Use minor version tags (e.g., `21-jre-jammy`)
- **Development:** Use major version tags (e.g., `21-jre`)
- **Never:** Use `latest` tag in production

### 3.3 Image Pinning

```dockerfile
# GOOD: Pinned digest
FROM eclipse-temurin:21-jre-jammy@sha256:abc123...

# GOOD: Pinned version
FROM eclipse-temurin:21-jre-jammy:21.0.3_9-jre

# BAD: Floating tag
FROM eclipse-temurin:21-jre:latest
```

## 4. Dockerfile Standards

### 4.1 Multi-Stage Build

```dockerfile
# Build stage
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

# Copy dependency files first for caching
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon

# Copy source and build
COPY platform/ platform/
COPY business/ business/
COPY ai/ ai/
COPY integration/ integration/
COPY libraries/ libraries/
RUN ./gradlew bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create non-root user
RUN groupadd -r -g 1001 appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Copy JAR from builder
COPY --from=builder --chown=appuser:appgroup /app/build/libs/*.jar app.jar

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD /app/healthcheck.sh || exit 1

# Run application
ENTRYPOINT ["/app/entrypoint.sh"]
```

### 4.2 Layer Optimization

```dockerfile
# GOOD: Dependencies cached separately
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon
COPY platform/ platform/
COPY business/ business/
COPY ai/ ai/
COPY integration/ integration/
COPY libraries/ libraries/
RUN ./gradlew bootJar --no-daemon

# BAD: Everything copied at once
COPY . .
RUN ./gradlew bootJar --no-daemon
```

### 4.3 Security Hardening

```dockerfile
# Use specific user, not root
RUN groupadd -r -g 1001 appgroup && \
    adduser -u 1001 -S appuser -G appgroup
USER appuser

# Read-only root filesystem
RUN mkdir -p /app/tmp && chown appuser:appgroup /app/tmp

# No new privileges (set in Kubernetes securityContext)
# Drop all capabilities (set in Kubernetes securityContext)
```

### 4.4 Environment Variables

```dockerfile
# Use ARG for build-time variables
ARG JAVA_VERSION=21
ARG APP_VERSION=1.0.0

# Use ENV for runtime variables with defaults
ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

# Allow override at runtime
ENV JAVA_OPTS=""
```

## 5. Docker Compose Standards

### 5.1 Service Definition

```yaml
version: '3.9'

services:
  finance-service:
    build:
      context: .
      dockerfile: business/finance/interfaces/Dockerfile
      args:
        SERVICE_NAME: finance-service
        SERVICE_MODULE: business:finance:interfaces
        APP_VERSION: ${APP_VERSION:-latest}
    image: erpai/finance-service:${APP_VERSION:-latest}
    container_name: finance-service
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - JAVA_OPTS=-Xmx512m -Xms256m
    env_file:
      - .env
    healthcheck:
      test: ["CMD", "/app/healthcheck.sh"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    networks:
      - erpai-network
    volumes:
      - finance-logs:/app/logs
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M

volumes:
  finance-logs:
    driver: local

networks:
  erpai-network:
    driver: bridge
```

### 5.2 Environment Variables

```bash
# .env.example
# Application
APP_NAME=erp-ai-platform
APP_VERSION=1.0.0
SPRING_PROFILES_ACTIVE=prod

# JVM
JAVA_OPTS=-Xmx512m -Xms256m

# Registry
REGISTRY=registry.example.com
```

## 6. Security Standards

### 6.1 Image Scanning

```bash
# Scan with Trivy
trivy image erpai/finance-service:1.0.0

# Scan with Grype
grype erpai/finance-service:1.0.0

# Fail on high/critical vulnerabilities
trivy image --severity HIGH,CRITICAL erpai/finance-service:1.0.0
```

### 6.2 Security Best Practices

- **Non-root user:** Always run as non-root (UID 1001)
- **Read-only filesystem:** Mount writable volumes only where needed
- **No secrets in images:** Use environment variables or secrets management
- **Minimal packages:** Install only required packages
- **Regular updates:** Update base images regularly
- **Vulnerability scanning:** Scan images in CI/CD

### 6.3 Dockerfile Security

```dockerfile
# Don't run as root
USER 1001

# Don't expose sensitive ports
# EXPOSE 22  # BAD - SSH
EXPOSE 8080  # OK - application

# Don't store secrets
# ENV API_KEY=secret  # BAD
# Use secrets or env vars at runtime
```

## 7. Health Checks

### 7.1 Application Health

```dockerfile
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD /app/healthcheck.sh || exit 1
```

### 7.2 Spring Boot Actuator

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true
```

### 7.3 Health Check Script

The health check script (`infrastructure/docker/healthcheck.sh`) supports:
- Configurable URL, timeout, retries, and delay
- Fallback between wget and curl
- Detailed logging for debugging

## 8. Logging

### 8.1 Log Configuration

```dockerfile
# Create log directory
RUN mkdir -p /app/logs && chown appuser:appgroup /app/logs
VOLUME ["/app/logs"]

# Log to stdout (Docker best practice)
ENV LOGGING_FILE_NAME=/app/logs/app.log
```

### 8.2 Log Driver

```yaml
services:
  finance-service:
    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "3"
        labels: "service,environment"
        env: "SERVICE_NAME,ENVIRONMENT"
```

## 9. Resource Limits

### 9.1 Docker Compose

```yaml
services:
  finance-service:
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

### 9.2 Kubernetes

```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "500m"
  limits:
    memory: "1Gi"
    cpu: "1000m"
```

## 10. Networking

### 10.1 Network Modes

- **Bridge:** Default, isolated network
- **Host:** Only for performance-critical services (avoid)
- **None:** No network access (for batch jobs)

### 10.2 Port Exposure

```dockerfile
# Only expose necessary ports
EXPOSE 8080

# Don't expose database ports externally
# EXPOSE 5432  # BAD - database should not be exposed
```

## 11. Volume Management

### 11.1 Named Volumes

```yaml
volumes:
  finance-logs:
    driver: local
```

### 11.2 Bind Mounts (Development Only)

```yaml
services:
  finance-service:
    volumes:
      - ./src:/app/src  # Development only
```

## 12. Dockerignore

```dockerignore
# .dockerignore
# Build artifacts
build/
out/
target/

# IDE
.idea/
.vscode/
*.iml

# Git
.git/
.gitignore

# Documentation
*.md
!README.md

# Tests (if not needed in image)
src/test/
**/*Test.java

# Gradle
.gradle/
gradlew
gradlew.bat
settings.gradle
build.gradle

# Environment
.env
.env.*
!.env.example

# OS
.DS_Store
Thumbs.db

# Temporary files
*.tmp
*.temp
*.log
```

## 13. CI/CD Integration

### 13.1 GitHub Actions

```yaml
name: Docker Build

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: .
          file: ./business/finance/interfaces/Dockerfile
          push: true
          tags: erpai/finance-service:${{ github.sha }}
          cache-from: type=registry,ref=erpai/finance-service:buildcache
          cache-to: type=registry,ref=erpai/finance-service:buildcache,mode=max

      - name: Scan image
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: erpai/finance-service:${{ github.sha }}
          format: 'sarif'
          output: 'trivy-results.sarif'

      - name: Upload scan results
        uses: github/codeql-action/upload-sarif@v2
        with:
          sarif_file: 'trivy-results.sarif'
```

## 14. Docker Checklist

- [ ] Multi-stage build used
- [ ] Non-root user configured (UID 1001)
- [ ] Base image pinned to version
- [ ] Layer caching optimized
- [ ] Health check configured
- [ ] Resource limits set
- [ ] No secrets in image
- [ ] Image scanned for vulnerabilities
- [ ] .dockerignore configured
- [ ] Logs go to stdout/stderr
- [ ] Graceful shutdown supported
- [ ] Health endpoint exposed
- [ ] Java 21 runtime used
- [ ] JVM container optimizations applied
