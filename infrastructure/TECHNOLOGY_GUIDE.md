# ERP AI Platform - Infrastructure Technology Guide

This document explains the infrastructure technologies used in the ERP AI Platform, their purposes, and how to use them in real project scenarios.

## Table of Contents

- [Docker & Docker Compose](#docker--docker-compose)
- [PostgreSQL](#postgresql)
- [Redis](#redis)
- [Apache Kafka](#apache-kafka)
- [Keycloak](#keycloak)
- [MinIO](#minio)
- [Prometheus](#prometheus)
- [Grafana](#grafana)
- [Tempo](#tempo)
- [Loki](#loki)
- [Terraform](#terraform)
- [Kubernetes](#kubernetes)

---

## Docker & Docker Compose

### Why It's Used
Docker provides containerization for consistent deployment across environments. Docker Compose orchestrates multi-container applications for local development.

### What It Does
- **Isolation**: Each service runs in its own container with defined dependencies
- **Reproducibility**: Identical environments from development to production
- **Portability**: Works on any system with Docker installed

### Real Project Scenario
When a developer joins the team, they can start the entire platform with a single command:

```bash
# Start all core infrastructure services
docker compose -f compose.base.yml -f compose.infrastructure.yml up -d

# Start with development tools (pgAdmin for database management)
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.development.yml --profile development up -d

# Start with monitoring stack
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.monitoring.yml --profile monitoring up -d
```

### Key Commands

```bash
# Build a microservice Docker image
./infrastructure/docker/build-service.sh finance-service business:finance:interfaces 1.0.0

# Check running services
docker compose ps

# View logs for a specific service
docker compose logs -f postgres

# Stop all services
docker compose down

# Remove all volumes (complete cleanup)
docker compose down -v
```

---

## PostgreSQL

### Why It's Used
PostgreSQL is the primary relational database for all business data. It's chosen for:
- ACID compliance for financial data integrity
- JSONB support for flexible document storage
- Extensibility with custom extensions
- Strong performance with complex queries

### What It Does
- Stores all business data across 9 separate databases (one per microservice)
- Provides transactional guarantees for financial operations
- Enables full-text search and similarity matching

### Real Project Scenario
The Finance service needs to store invoices and payments. When the platform starts:

```bash
# PostgreSQL automatically creates databases on first run
# 01-create-databases.sql creates: erpai_finance, erpai_hr, etc.
# 02-create-users.sql creates dedicated users with least-privilege access
# 03-create-extensions.sql enables uuid-ossp, pg_trgm, pgcrypto, etc.

# Connect to the finance database
docker compose exec postgres psql -U erpai -d erpai_finance

# Run migrations to create tables
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.development.yml --profile development up flyway
```

### Key Commands

```bash
# Connect to PostgreSQL
docker compose exec postgres psql -U erpai -d erpai_platform

# Create a backup
bash infrastructure/postgres/backups/backup.sh

# Check database sizes
docker compose exec postgres psql -U erpai -d erpai_platform -c "
  SELECT datname, pg_size_pretty(pg_database_size(datname)) 
  FROM pg_database 
  WHERE datname LIKE 'erpai_%';"
```

---

## Redis

### Why It's Used
Redis serves as the distributed cache and session store. It's chosen for:
- Sub-millisecond response times
- Support for data structures (lists, sets, sorted sets)
- Built-in replication and persistence
- Pub/Sub for real-time notifications

### What It Does
- Caches frequently accessed data (product catalogs, user sessions)
- Stores rate limiting counters for API gateway
- Enables distributed locking for concurrent operations

### Real Project Scenario
The Inventory service needs to cache product information to reduce database load:

```bash
# Redis is configured with password authentication and persistence
# The service uses Redis for:
# - Product stock level caching (updated on every sale)
# - Session storage for user authentication
# - Rate limiting for API endpoints

# Test Redis connectivity
docker compose exec redis redis-cli ping

# View cache statistics
docker compose exec redis redis-cli info stats
```

### Key Commands

```bash
# Connect to Redis CLI
docker compose exec redis redis-cli -a erpai_dev_password

# Clear all cache (useful during development)
docker compose exec redis redis-cli -a erpai_dev_password FLUSHALL

# Monitor real-time commands
docker compose exec redis redis-cli -a erpai_dev_password MONITOR
```

---

## Apache Kafka

### Why It's Used
Kafka is the event streaming platform for inter-service communication. It's chosen for:
- High-throughput, fault-tolerant message processing
- Event sourcing and CQRS patterns
- Real-time data pipelines
- Decoupling services through events

### What It Does
- Transports domain events between microservices
- Enables event-driven architecture
- Provides replay capability for event sourcing

### Real Project Scenario
When a sales order is created, the Sales service publishes an event:

```bash
# Kafka configuration includes:
# - 12 partitions for parallel processing
# - 7-day retention for event replay
# - KRaft mode (no Zookeeper required)

# Create a topic for sales events
docker compose exec kafka \
  kafka-topics --bootstrap-server kafka:9092 \
  --create --topic sales.orders \
  --partitions 6 --replication-factor 1

# Consume events from a topic
docker compose exec kafka \
  kafka-console-consumer --bootstrap-server kafka:9092 \
  --topic sales.orders --from-beginning
```

### Key Commands

```bash
# List all topics
docker compose exec kafka kafka-topics --bootstrap-server kafka:9092 --list

# Describe a topic
docker compose exec kafka kafka-topics --bootstrap-server kafka:9092 --describe --topic sales.orders

# Produce a test message
docker compose exec kafka kafka-console-producer --bootstrap-server kafka:9092 --topic sales.orders

# Check consumer groups
docker compose exec kafka kafka-consumer-groups --bootstrap-server kafka:9092 --list
```

---

## Keycloak

### Why It's Used
Keycloak is the identity and access management solution. It's chosen for:
- Single Sign-On (SSO) across all services
- OAuth2/OpenID Connect compliance
- User federation (LDAP, Active Directory)
- Fine-grained authorization policies

### What It Does
- Authenticates users and services
- Issues JWT tokens for API access
- Manages user roles and permissions
- Provides admin console for user management

### Real Project Scenario
A user logs into the ERP system:

```bash
# Keycloak is pre-configured with:
# - Password policy: 12 chars, digits, upper/lower case, special chars
# - Session timeout: 30 minutes idle, 1 hour max
# - Brute force protection: 5 attempts, 15 min lockout

# Get an access token
curl -X POST http://localhost:8080/realms/erpai/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" \
  -d "client_id=erpai-gateway"

# Access the admin console
open http://localhost:8080/
```

### Key Commands

```bash
# Check Keycloak health
curl http://localhost:8080/health/ready

# Export realm configuration
docker compose exec keycloak /opt/keycloak/bin/kc.sh export --optimized --file /tmp/realm.json

# View realm info
curl http://localhost:8080/realms/erpai/.well-known/openid-configuration
```

---

## MinIO

### Why It's Used
MinIO is the S3-compatible object storage. It's chosen for:
- S3 API compatibility (no vendor lock-in)
- High performance for large files
- Multi-tenant support
- Kubernetes-native deployment

### What It Does
- Stores document attachments (invoices, contracts)
- Stores AI model artifacts
- Stores backup files
- Provides presigned URLs for secure uploads

### Real Project Scenario
The Finance service needs to store invoice PDFs:

```bash
# MinIO is configured with:
# - Two ports: 9000 (API), 9001 (Console)
# - Default credentials: minioadmin/minioadmin

# Create a bucket for invoices
docker compose exec minio \
  mc alias set myminio http://localhost:9000 minioadmin minioadmin && \
  mc mb myminio/finance-invoices

# Upload a file
docker compose exec minio \
  mc cp /data/invoice.pdf myminio/finance-invoices/

# Generate a presigned URL for upload
docker compose exec minio \
  mc share upload myminio/finance-invoices --expire 7d
```

### Key Commands

```bash
# Access MinIO console
open http://localhost:9001

# List buckets
docker compose exec minio mc ls myminio

# Download an object
docker compose exec minio mc cp myminio/finance-invoices/invoice.pdf /tmp/

# Set bucket policy
docker compose exec minio mc policy set public myminio/finance-invoices
```

---

## Prometheus

### Why It's Used
Prometheus is the metrics collection and monitoring system. It's chosen for:
- Pull-based metrics collection
- Multi-dimensional data model with labels
- Powerful query language (PromQL)
- Integration with Grafana for visualization

### What It Does
- Scrapes metrics from all services
- Stores time-series data for 30 days
- Enables alerting on metric thresholds
- Provides service discovery for dynamic targets

### Real Project Scenario
Monitor the health of the Sales service:

```bash
# Prometheus scrapes:
# - JVM metrics (heap, GC, threads)
# - HTTP request counts and latencies
# - Database connection pool metrics
# - Custom business metrics

# Query average response time
# http://localhost:9090/graph
# rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])

# Check if services are up
# up{job="gateway"}
```

### Key Commands

```bash
# Access Prometheus UI
open http://localhost:9090

# Query metrics
curl -G http://localhost:9090/api/v1/query --data-urlencode 'query=up'

# Check target status
curl http://localhost:9090/api/v1/targets

# Reload configuration
curl -X POST http://localhost:9090/-/reload
```

---

## Grafana

### Why It's Used
Grafana provides visualization and dashboard capabilities. It's chosen for:
- Rich dashboard creation
- Multiple data source support
- Alerting and notification
- Team collaboration features

### What It Does
- Visualizes metrics from Prometheus
- Displays traces from Tempo
- Shows logs from Loki
- Creates business dashboards

### Real Project Scenario
Create a dashboard for sales performance:

```bash
# Grafana is pre-configured with:
# - Prometheus data source
# - Tempo data source for traces
# - Loki data source for logs

# Access Grafana
open http://localhost:3000
# Login: admin/admin (or set GRAFANA_ADMIN_PASSWORD env var)

# Import dashboards for:
# - Service health (JVM, HTTP, DB metrics)
# - Business KPIs (orders, revenue, inventory levels)
# - Infrastructure (CPU, memory, disk usage)
```

### Key Commands

```bash
# Access Grafana
open http://localhost:3000

# Provisioning API
curl -X GET http://localhost:3000/api/datasources

# Create dashboard via API
curl -X POST http://localhost:3000/api/dashboards/db \
  -H "Content-Type: application/json" \
  -d @dashboard.json
```

---

## Tempo

### Why It's Used
Tempo is the distributed tracing backend. It's chosen for:
- High-scale trace collection
- Integration with Grafana
- Cost-effective storage
- OpenTelemetry compatibility

### What It Does
- Collects distributed traces from services
- Correlates requests across microservices
- Enables performance debugging
- Integrates with logs via trace IDs

### Real Project Scenario
Debug a slow order creation:

```bash
# Tempo receives traces via OTLP:
# - Port 4317: OTLP gRPC
# - Port 4318: OTLP HTTP
# - Port 3200: Query API

# Search traces in Grafana:
# - Service: sales-service
# - Operation: CreateOrder
# - Min duration: 1s
```

### Key Commands

```bash
# Check Tempo status
curl http://localhost:3200/ready

# Search traces
curl -X GET "http://localhost:3200/api/search?service=sales-service&tags=error:true"
```

---

## Loki

### Why It's Used
Loki is the log aggregation system. It's chosen for:
- Cost-effective log storage
- Integration with Grafana
- Label-based indexing
- Kubernetes-native

### What It Does
- Collects logs from all containers
- Indexes logs by labels (service, level, etc.)
- Enables log search and filtering
- Correlates logs with traces

### Real Project Scenario
Find error logs for a specific order:

```bash
# Loki receives logs via Promtail or Docker driver
# Query in Grafana:
# {service="sales-service"} |= "order-12345"

# Or via API:
curl -G "http://localhost:3100/loki/api/v1/query_range" \
  --data-urlencode 'query={service="sales-service"} |= "ERROR"'
```

### Key Commands

```bash
# Check Loki status
curl http://localhost:3100/ready

# Query logs
curl -G "http://localhost:3100/loki/api/v1/query" \
  --data-urlencode 'query={job="erpai-platform"}'
```

---

## Terraform

### Why It's Used
Terraform provides Infrastructure as Code (IaC) for AWS. It's chosen for:
- Declarative infrastructure definition
- State management and drift detection
- Multi-cloud provider support
- Module reusability

### What It Does
- Provisions AWS infrastructure (VPC, EKS, RDS, etc.)
- Manages Kubernetes resources
- Deploys Helm charts
- Creates S3 buckets for backups

### Real Project Scenario
Deploy the platform to AWS:

```bash
# The Terraform configuration defines:
# - VPC with public/private subnets
# - EKS cluster with node groups
# - RDS PostgreSQL (multi-AZ for production)
# - ElastiCache Redis (cluster mode)
# - MSK Kafka cluster
# - S3 buckets for assets, models, backups

# Initialize and deploy
cd infrastructure/terraform
terraform init
terraform plan -var-file="environments/dev.tfvars"
terraform apply -var-file="environments/dev.tfvars"
```

### Key Commands

```bash
# Initialize Terraform
terraform init

# Plan changes
terraform plan -out=tfplan

# Apply changes
terraform apply tfplan

# Destroy infrastructure
terraform destroy -var-file="environments/dev.tfvars"

# Show current state
terraform show
```

---

## Kubernetes

### Why It's Used
Kubernetes orchestrates containerized applications in production. It's chosen for:
- Automatic scaling and failover
- Service discovery and load balancing
- Rolling updates and rollbacks
- Resource management

### What It Does
- Runs all infrastructure services
- Manages application deployments
- Handles service networking
- Provides persistent storage

### Real Project Scenario
Deploy the platform to a Kubernetes cluster:

```bash
# The Kubernetes manifests define:
# - Namespace for isolation
# - ConfigMaps for configuration
# - Secrets for sensitive data
# - StatefulSets for databases
# - Deployments for stateless services
# - Services for internal networking
# - Ingress for external access

# Deploy to Kubernetes
kubectl apply -f infrastructure/k8s/namespace.yaml
kubectl apply -f infrastructure/k8s/configmap.yaml
kubectl apply -f infrastructure/k8s/postgres/
kubectl apply -f infrastructure/k8s/redis/
kubectl apply -f infrastructure/k8s/kafka/
kubectl apply -f infrastructure/k8s/minio/
kubectl apply -f infrastructure/k8s/keycloak/
```

### Key Commands

```bash
# Apply all manifests
kubectl apply -f infrastructure/k8s/

# Check pod status
kubectl get pods -n erpai-platform

# View logs
kubectl logs -f deployment/gateway -n erpai-platform

# Port forward for local access
kubectl port-forward svc/gateway 8080:80 -n erpai-platform

# Scale a deployment
kubectl scale deployment gateway --replicas=3 -n erpai-platform
```

---

## Quick Reference: Starting the Platform

```bash
# 1. Start core infrastructure
docker compose -f compose.base.yml -f compose.infrastructure.yml up -d

# 2. Start development tools
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.development.yml --profile development up -d

# 3. Start monitoring
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.monitoring.yml --profile monitoring up -d

# 4. Run database migrations
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.development.yml --profile development up flyway

# 5. Access services
# - API Gateway: http://localhost:8080
# - Keycloak: http://localhost:8080 (admin console)
# - pgAdmin: http://localhost:5050
# - Grafana: http://localhost:3000
# - Prometheus: http://localhost:9090
# - MinIO: http://localhost:9001
```

---

## Command Reference

This section provides a detailed explanation of all commands used in this guide, especially useful for first-time users.

### Docker & Docker Compose Commands

| Command | Description |
|---------|-------------|
| `docker compose -f compose.base.yml -f compose.infrastructure.yml up -d` | Starts all core infrastructure services (PostgreSQL, Redis, Kafka, Keycloak, MinIO) in detached mode (background). The `-f` flag specifies multiple compose files to merge. |
| `docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.development.yml --profile development up -d` | Starts core infrastructure plus development tools (like pgAdmin for database management) using the "development" profile. |
| `docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.monitoring.yml --profile monitoring up -d` | Starts core infrastructure plus the monitoring stack (Prometheus, Grafana, Tempo, Loki) using the "monitoring" profile. |
| `./infrastructure/docker/build-service.sh finance-service business:finance:interfaces 1.0.0` | Builds a Docker image for a specific microservice. Parameters: service name, image tag, and version. |
| `docker compose ps` | Lists all running containers with their status, ports, and names. Useful to verify services are running. |
| `docker compose logs -f postgres` | Shows real-time logs for the PostgreSQL container. The `-f` flag follows the log output (like `tail -f`). |
| `docker compose down` | Stops and removes all containers, networks, and volumes created by `docker compose up`. |
| `docker compose down -v` | Stops and removes all containers, networks, AND volumes. Use this for a complete cleanup when you want to start fresh. |

### PostgreSQL Commands

| Command | Description |
|---------|-------------|
| `docker compose exec postgres psql -U erpai -d erpai_platform` | Connects to the PostgreSQL database using the `psql` CLI. `-U` specifies the username, `-d` specifies the database name. |
| `docker compose exec postgres psql -U erpai -d erpai_finance` | Connects to the finance-specific database. Each microservice has its own database (e.g., `erpai_finance`, `erpai_hr`). |
| `docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.development.yml --profile development up flyway` | Runs Flyway database migrations inside a temporary container to create/update database schemas. |
| `bash infrastructure/postgres/backups/backup.sh` | Executes the backup script to create a backup of all PostgreSQL databases. |
| `docker compose exec postgres psql -U erpai -d erpai_platform -c "SELECT datname, pg_size_pretty(pg_database_size(datname)) FROM pg_database WHERE datname LIKE 'erpai_%';"` | Queries all databases matching the `erpai_` pattern and displays their sizes in human-readable format. |

### Redis Commands

| Command | Description |
|---------|-------------|
| `docker compose exec redis redis-cli ping` | Tests connectivity to Redis. Returns `PONG` if Redis is responding. |
| `docker compose exec redis redis-cli info stats` | Displays Redis statistics including hits, misses, keyspace hits, and other performance metrics. |
| `docker compose exec redis redis-cli -a erpai_dev_password` | Connects to Redis CLI with password authentication. The `-a` flag provides the password. |
| `docker compose exec redis redis-cli -a erpai_dev_password FLUSHALL` | **Dangerous!** Deletes all data from all databases in Redis. Useful during development to clear cache. |
| `docker compose exec redis redis-cli -a erpai_dev_password MONITOR` | Shows all commands being processed by Redis in real-time. Useful for debugging cache behavior. |

### Apache Kafka Commands

| Command | Description |
|---------|-------------|
| `docker compose exec kafka kafka-topics --bootstrap-server kafka:9092 --create --topic sales.orders --partitions 6 --replication-factor 1` | Creates a new Kafka topic named `sales.orders` with 6 partitions and 1 replica. Partitions allow parallel processing. |
| `docker compose exec kafka kafka-console-consumer --bootstrap-server kafka:9092 --topic sales.orders --from-beginning` | Reads messages from the `sales.orders` topic starting from the beginning. Useful for testing and debugging. |
| `docker compose exec kafka kafka-topics --bootstrap-server kafka:9092 --list` | Lists all existing Kafka topics. |
| `docker compose exec kafka kafka-topics --bootstrap-server kafka:9092 --describe --topic sales.orders` | Shows detailed information about a topic including partition count, replication factor, and ISR (In-Sync Replicas). |
| `docker compose exec kafka kafka-console-producer --bootstrap-server kafka:9092 --topic sales.orders` | Opens an interactive prompt to produce (send) messages to a topic. Type a message and press Enter to send. |
| `docker compose exec kafka kafka-consumer-groups --bootstrap-server kafka:9092 --list` | Lists all consumer groups. Consumer groups are used to track which services are consuming from which topics. |

### Keycloak Commands

| Command | Description |
|---------|-------------|
| `curl -X POST http://localhost:8080/realms/erpai/protocol/openid-connect/token -H "Content-Type: application/x-www-form-urlencoded" -d "username=admin" -d "password=admin" -d "grant_type=password" -d "client_id=erpai-gateway"` | Obtains an access token (JWT) from Keycloak using the OAuth2 password grant. This token is used to authenticate API requests. |
| `open http://localhost:8080/` | Opens the Keycloak admin console in your default browser. Use this to manage users, roles, and client configurations. |
| `curl http://localhost:8080/health/ready` | Checks if Keycloak is ready to accept requests. Returns HTTP 200 when healthy. |
| `docker compose exec keycloak /opt/keycloak/bin/kc.sh export --optimized --file /tmp/realm.json` | Exports the Keycloak realm configuration to a JSON file. Useful for backup or migration. |
| `curl http://localhost:8080/realms/erpai/.well-known/openid-configuration` | Retrieves the OpenID Connect configuration for the `erpai` realm. Contains endpoints for token issuance, user info, etc. |

### MinIO Commands

| Command | Description |
|---------|-------------|
| `docker compose exec minio mc alias set myminio http://localhost:9000 minioadmin minioadmin && mc mb myminio/finance-invoices` | Configures the MinIO client (`mc`) with an alias and creates a new bucket named `finance-invoices`. Buckets are like folders for storing objects. |
| `docker compose exec minio mc cp /data/invoice.pdf myminio/finance-invoices/` | Uploads a file (`invoice.pdf`) to the `finance-invoices` bucket. |
| `docker compose exec minio mc share upload myminio/finance-invoices --expire 7d` | Generates a presigned URL for uploading files. The URL expires in 7 days. Presigned URLs allow secure, temporary access without exposing credentials. |
| `open http://localhost:9001` | Opens the MinIO web console in your browser. Use this to browse buckets, upload/download files, and manage permissions. |
| `docker compose exec minio mc ls myminio` | Lists all buckets in MinIO. |
| `docker compose exec minio mc cp myminio/finance-invoices/invoice.pdf /tmp/` | Downloads a file from MinIO to the local filesystem (`/tmp/`). |
| `docker compose exec minio mc policy set public myminio/finance-invoices` | Sets the bucket policy to `public`, making all objects in the bucket publicly accessible. Use with caution! |

### Prometheus Commands

| Command | Description |
|---------|-------------|
| `open http://localhost:9090` | Opens the Prometheus web UI. Use this to run PromQL queries and view metrics. |
| `curl -G http://localhost:9090/api/v1/query --data-urlencode 'query=up'` | Queries Prometheus via its HTTP API. The example query `up` returns the status of all monitored targets (1 = up, 0 = down). |
| `curl http://localhost:9090/api/v1/targets` | Retrieves information about all scrape targets (services being monitored). Shows if they are up or down. |
| `curl -X POST http://localhost:9090/-/reload` | Tells Prometheus to reload its configuration file without restarting. Useful when you update `prometheus.yml`. |

### Grafana Commands

| Command | Description |
|---------|-------------|
| `open http://localhost:3000` | Opens the Grafana dashboard UI. Default credentials are `admin/admin` (or set via `GRAFANA_ADMIN_PASSWORD`). |
| `curl -X GET http://localhost:3000/api/datasources` | Lists all configured data sources in Grafana (e.g., Prometheus, Tempo, Loki) via the HTTP API. |
| `curl -X POST http://localhost:3000/api/dashboards/db -H "Content-Type: application/json" -d @dashboard.json` | Creates a new dashboard in Grafana using a JSON definition file. Useful for automating dashboard provisioning. |

### Tempo Commands

| Command | Description |
|---------|-------------|
| `curl http://localhost:3200/ready` | Checks if Tempo is ready to accept trace data. Returns HTTP 200 when healthy. |
| `curl -X GET "http://localhost:3200/api/search?service=sales-service&tags=error:true"` | Searches for traces from the `sales-service` that have an `error:true` tag. Useful for finding failed requests. |

### Loki Commands

| Command | Description |
|---------|-------------|
| `curl http://localhost:3100/ready` | Checks if Loki is ready to accept log data. Returns HTTP 200 when healthy. |
| `curl -G "http://localhost:3100/loki/api/v1/query_range" --data-urlencode 'query={service="sales-service"} \|= "ERROR"'` | Queries Loki for log lines containing "ERROR" from the `sales-service` over a time range. The `|=` operator means "contains". |

### Terraform Commands

| Command | Description |
|---------|-------------|
| `cd infrastructure/terraform` | Changes directory to the Terraform configuration folder. All Terraform commands should be run from this directory. |
| `terraform init` | Initializes a Terraform working directory. Downloads provider plugins and sets up the backend. Run this once before any other Terraform command. |
| `terraform plan -var-file="environments/dev.tfvars"` | Creates an execution plan showing what Terraform will do. The `-var-file` flag loads variables from a file (e.g., environment-specific settings). |
| `terraform plan -out=tfplan` | Creates an execution plan and saves it to a file (`tfplan`). This plan can be reviewed and then applied. |
| `terraform apply -var-file="environments/dev.tfvars"` | Applies the changes to reach the desired state. This actually creates/updates/destroys infrastructure. |
| `terraform apply tfplan` | Applies a previously saved execution plan (`tfplan`). This ensures exactly what was planned gets applied. |
| `terraform destroy -var-file="environments/dev.tfvars"` | Destroys all infrastructure managed by Terraform. **Use with extreme caution!** This is irreversible. |
| `terraform show` | Displays the current state of the infrastructure. Shows all resources and their attributes. |

### Kubernetes Commands

| Command | Description |
|---------|-------------|
| `kubectl apply -f infrastructure/k8s/namespace.yaml` | Applies (creates/updates) a Kubernetes resource from a YAML file. In this case, creates the `erpai-platform` namespace. |
| `kubectl apply -f infrastructure/k8s/configmap.yaml` | Applies a ConfigMap resource, which stores non-sensitive configuration data (like environment variables). |
| `kubectl apply -f infrastructure/k8s/postgres/` | Applies all Kubernetes manifests in the `postgres/` directory (StatefulSet, Service, PersistentVolumeClaim, etc.). |
| `kubectl apply -f infrastructure/k8s/redis/` | Applies all Kubernetes manifests in the `redis/` directory. |
| `kubectl apply -f infrastructure/k8s/kafka/` | Applies all Kubernetes manifests in the `kafka/` directory. |
| `kubectl apply -f infrastructure/k8s/minio/` | Applies all Kubernetes manifests in the `minio/` directory. |
| `kubectl apply -f infrastructure/k8s/keycloak/` | Applies all Kubernetes manifests in the `keycloak/` directory. |
| `kubectl apply -f infrastructure/k8s/` | Applies all Kubernetes manifests in the entire `k8s/` directory. |
| `kubectl get pods -n erpai-platform` | Lists all pods in the `erpai-platform` namespace. Shows pod name, status, restarts, and age. |
| `kubectl logs -f deployment/gateway -n erpai-platform` | Shows real-time logs from the `gateway` deployment. The `-f` flag follows the log output. |
| `kubectl port-forward svc/gateway 8080:80 -n erpai-platform` | Forwards local port 8080 to port 80 of the `gateway` service. Allows local access to a service running in the cluster. |
| `kubectl scale deployment gateway --replicas=3 -n erpai-platform` | Scales the `gateway` deployment to 3 replicas (pods). Useful for handling increased traffic. |

### General Utility Commands

| Command | Description |
|---------|-------------|
| `open <url>` | Opens a URL in your default web browser (macOS only). On Linux, use `xdg-open` instead. |
| `curl <url>` | Transfers data from or to a server using HTTP. Commonly used to test APIs and check service health. |
| `curl -X POST` | Sends an HTTP POST request. Used for actions that create or modify data (like getting a token). |
| `curl -X GET` | Sends an HTTP GET request. Used for retrieving data. |
| `curl -G` | Sends an HTTP GET request with query parameters. The `--data-urlencode` flag is often used with this to encode parameters. |
| `bash <script>` | Executes a shell script. Used to run automation scripts like backups. |
| `cd <directory>` | Changes the current working directory. Used to navigate into project folders before running commands. |
