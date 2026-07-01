# Docker Standards

## 1. Purpose

These standards ensure consistent, secure, and efficient containerization across the ERP SaaS platform. All Dockerfiles and container configurations must follow these guidelines.

## 2. General Principles

- **Minimal images:** Use smallest possible base images
- **Security:** Run as non-root, scan for vulnerabilities
- **Layer caching:** Optimize Dockerfile layer ordering
- **Reproducibility:** Pin versions, use digests
- **Multi-stage builds:** Separate build and runtime environments

## 3. Base Image Standards

### 3.1 Approved Base Images

| Image | Use Case | Tag Pattern |
|-------|----------|-------------|
| `eclipse-temurin:17-jre` | Java runtime | `17-jre` or `17-jre-alpine` |
| `eclipse-temurin:17-jdk` | Java build | `17-jdk` or `17-jdk-alpine` |
| `postgres:15` | PostgreSQL | `15-alpine` |
| `redis:7-alpine` | Redis | `7-alpine` |
| `nginx:alpine` | Nginx proxy | `alpine` |
| `prom/prometheus` | Prometheus | `latest` |
| `grafana/grafana` | Grafana | `latest` |
| `confluentinc/cp-kafka` | Kafka | `latest` |

### 3.2 Image Tagging

- **Production:** Use specific version tags (e.g., `17-jre:17.0.9_11-jre`)
- **Staging:** Use minor version tags (e.g., `17-jre:17.0`)
- **Development:** Use major version tags (e.g., `17-jre:17`)
- **Never:** Use `latest` tag in production

### 3.3 Image Pinning

```dockerfile
# GOOD: Pinned digest
FROM eclipse-temurin:17-jre@sha256:abc123...

# GOOD: Pinned version
FROM eclipse-temurin:17-jre:17.0.9_11-jre

# BAD: Floating tag
FROM eclipse-temurin:17-jre:latest
```

## 4. Dockerfile Standards

### 4.1 Multi-Stage Build

```dockerfile
# Build stage
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copy dependency files first for caching
COPY build.gradle settings.gradle gradlew ./
COPY gradle gradle
RUN ./gradlew dependencies --no-daemon

# Copy source and build
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:17-jre-alpine:17.0.9_11-jre
WORKDIR /app

# Create non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Copy JAR from builder
COPY --from=builder --chown=appuser:appgroup /app/build/libs/*.jar app.jar

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 4.2 Layer Optimization

```dockerfile
# GOOD: Dependencies cached separately
COPY build.gradle settings.gradle gradlew ./
COPY gradle gradle
RUN ./gradlew dependencies --no-daemon
COPY src src
RUN ./gradlew bootJar --no-daemon

# BAD: Everything copied at once
COPY . .
RUN ./gradlew bootJar --no-daemon
```

### 4.3 Security Hardening

```dockerfile
# Use specific user, not root
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup
USER appuser

# Read-only root filesystem
RUN mkdir -p /tmp/app && chown appuser:appgroup /tmp/app
VOLUME ["/tmp/app"]

# No new privileges
# (Set in Kubernetes securityContext)

# Drop all capabilities
# (Set in Kubernetes securityContext)
```

### 4.4 Environment Variables

```dockerfile
# Use ARG for build-time variables
ARG JAVA_VERSION=17
ARG APP_VERSION=1.0.0

# Use ENV for runtime variables with defaults
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

# Allow override at runtime
ENV JAVA_OPTS=""
```

## 5. Docker Compose Standards

### 5.1 Service Definition

```yaml
version: '3.8'

services:
  invoice-service:
    build:
      context: .
      dockerfile: Dockerfile
      args:
        JAVA_VERSION: 17
        APP_VERSION: ${APP_VERSION:-latest}
    image: erp/invoice-service:${APP_VERSION:-latest}
    container_name: invoice-service
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/invoice
      - SPRING_REDIS_HOST=redis
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
    env_file:
      - .env
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      kafka:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    networks:
      - erp-network
    volumes:
      - invoice-logs:/app/logs
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M

  postgres:
    image: postgres:15-alpine
    container_name: postgres
    restart: unless-stopped
    environment:
      - POSTGRES_DB=erp
      - POSTGRES_USER=${POSTGRES_USER:-erp}
      - POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./infrastructure/docker/postgres/init:/docker-entrypoint-initdb.d
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-erp}"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - erp-network

  redis:
    image: redis:7-alpine
    container_name: redis
    restart: unless-stopped
    command: redis-server --appendonly yes
    volumes:
      - redis-data:/data
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - erp-network

  kafka:
    image: confluentinc/cp-kafka:latest
    container_name: kafka
    restart: unless-stopped
    environment:
      - KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181
      - KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092
      - KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1
    depends_on:
      zookeeper:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "kafka-broker-api-versions.sh", "--bootstrap-server", "localhost:9092"]
      interval: 30s
      timeout: 10s
      retries: 5
    networks:
      - erp-network

  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    container_name: zookeeper
    restart: unless-stopped
    environment:
      - ZOOKEEPER_CLIENT_PORT=2181
    healthcheck:
      test: ["CMD", "nc", "-z", "localhost", "2181"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - erp-network

volumes:
  postgres-data:
  redis-data:
  kafka-data:
  invoice-logs:

networks:
  erp-network:
    driver: bridge
```

### 5.2 Environment Variables

```bash
# .env.example
# Database
POSTGRES_DB=erp
POSTGRES_USER=erp
POSTGRES_PASSWORD=changeme

# Application
APP_VERSION=1.0.0
SPRING_PROFILES_ACTIVE=prod

# JVM
JAVA_OPTS=-Xmx512m -Xms256m

# External Services
KEYCLOAK_URL=http://keycloak:8080
MINIO_URL=http://minio:9000
```

## 6. Security Standards

### 6.1 Image Scanning

```bash
# Scan with Trivy
trivy image erp/invoice-service:1.0.0

# Scan with Grype
grype erp/invoice-service:1.0.0

# Fail on high/critical vulnerabilities
trivy image --severity HIGH,CRITICAL erp/invoice-service:1.0.0
```

### 6.2 Security Best Practices

- **Non-root user:** Always run as non-root
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
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
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
  invoice-service:
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
  invoice-service:
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
  postgres-data:
    driver: local
  redis-data:
    driver: local
```

### 11.2 Bind Mounts (Development Only)

```yaml
services:
  invoice-service:
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
docs/

# Tests (if not needed in image)
src/test/
**/*Test.java

# Gradle
gradle/
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
      - uses: actions/checkout@v3
      
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v2
      
      - name: Login to Docker Hub
        uses: docker/login-action@v2
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}
      
      - name: Build and push
        uses: docker/build-push-action@v4
        with:
          context: .
          file: ./Dockerfile
          push: true
          tags: erp/invoice-service:${{ github.sha }}
          cache-from: type=registry,ref=erp/invoice-service:buildcache
          cache-to: type=registry,ref=erp/invoice-service:buildcache,mode=max
      
      - name: Scan image
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: erp/invoice-service:${{ github.sha }}
          format: 'sarif'
          output: 'trivy-results.sarif'
      
      - name: Upload scan results
        uses: github/codeql-action/upload-sarif@v2
        with:
          sarif_file: 'trivy-results.sarif'
```

## 14. Docker Checklist

- [ ] Multi-stage build used
- [ ] Non-root user configured
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
