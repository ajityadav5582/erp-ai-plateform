# ERP AI Platform - Docker Infrastructure

Production-ready Docker foundation for Java 21 microservices.

## Directory Structure

```
infrastructure/docker/
├── .env.example              # Build environment variables
├── .gitignore                # Git ignore for Docker files
├── README.md                 # This file
├── Dockerfile.service        # Service-level Dockerfile template
├── build-service.sh          # Docker build script
├── entrypoint.sh             # Container entrypoint with signal handling
├── healthcheck.sh            # Health check script
├── jvm.config                # JVM runtime configuration
├── grafana/                  # Grafana provisioning
├── loki/                     # Loki configuration
├── prometheus/               # Prometheus configuration
└── tempo/                    # Tempo configuration
```

## Quick Start

### Building a Service

```bash
# Set environment variables
cp infrastructure/docker/.env.example infrastructure/docker/.env

# Build a service
infrastructure/docker/build-service.sh finance-service business:finance:interfaces 1.0.0

# Build and push to registry
REGISTRY=registry.example.com PUSH=true infrastructure/docker/build-service.sh finance-service business:finance:interfaces 1.0.0
```

### Using the Service Dockerfile

1. Copy `infrastructure/docker/Dockerfile.service` to your service's `interfaces/` directory
2. Update the `SERVICE_NAME` and `SERVICE_MODULE` build arguments
3. Adjust the `COPY` paths to match your service structure
4. Build the image:

```bash
docker build \
  --file business/finance/interfaces/Dockerfile \
  --tag finance-service:1.0.0 \
  --build-arg SERVICE_NAME=finance-service \
  --build-arg SERVICE_MODULE=business:finance:interfaces \
  --build-arg APP_VERSION=1.0.0 \
  .
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVICE_NAME` | Service name | Required |
| `SERVICE_MODULE` | Gradle module path | Required |
| `APP_VERSION` | Application version | `0.1.0-SNAPSHOT` |
| `JAVA_OPTS` | JVM options | Empty |
| `SPRING_PROFILES_ACTIVE` | Spring profile | `prod` |
| `SERVER_PORT` | Server port | `8080` |
| `REGISTRY` | Docker registry | Empty |
| `PUSH` | Push to registry | `false` |
| `CACHE_FROM` | Build cache source | Empty |
| `CACHE_TO` | Build cache target | Empty |
| `SCAN` | Run security scan | `false` |

### JVM Configuration

The JVM configuration is defined in `jvm.config` and applied via the entrypoint script:

- **Memory:** Container-aware with 75% max RAM, 50% initial RAM
- **GC:** G1GC with 200ms max pause time
- **Heap Dump:** On OOM, saved to `/app/tmp/heapdump.hprof`
- **Security:** Non-blocking entropy via `/dev/urandom`
- **Actuator:** Health, info, metrics, and Prometheus endpoints exposed

### Health Check

The health check script (`healthcheck.sh`) verifies application readiness:

- Checks the `/actuator/health/readiness` endpoint
- Configurable timeout, retries, and delay
- Falls back between wget and curl
- Returns non-zero exit code on failure

### Entrypoint

The entrypoint script (`entrypoint.sh`) provides:

- Signal handling for graceful shutdown (SIGTERM, SIGINT)
- JVM container optimizations
- Environment variable expansion
- Proper process replacement for signal forwarding

## Security

### Non-Root Containers

All containers run as UID 1001 (non-root):

```dockerfile
RUN groupadd -r -g 1001 appgroup && \
    useradd -r -u 1001 -g appgroup -m -d /home/appuser appuser
USER appuser
```

### Image Scanning

Scan images for vulnerabilities:

```bash
# Using Trivy
trivy image erpai/finance-service:1.0.0

# Using Grype
grype erpai/finance-service:1.0.0
```

## Build Optimization

### Layer Caching

The Dockerfile is optimized for layer caching:

1. **Gradle files** copied first (changes infrequently)
2. **Dependencies** downloaded and cached
3. **Source code** copied last (changes frequently)

### Build Cache

Use Docker BuildKit cache for faster rebuilds:

```bash
# Build with cache
docker build \
  --cache-from type=registry,ref=erpai/finance-service:buildcache \
  --cache-to type=registry,ref=erpai/finance-service:buildcache,mode=max \
  -t erpai/finance-service:1.0.0 .
```

## Documentation

See [`docs/standards/16-docker-standards.md`](../docs/standards/16-docker-standards.md) for complete Docker standards.
