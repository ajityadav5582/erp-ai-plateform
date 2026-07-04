# Monitoring Infrastructure

Enterprise-grade monitoring stack for the ERP AI Platform using Prometheus, Grafana, Jaeger, Tempo, and Loki.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Monitoring Stack                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐  │
│  │ Prometheus  │    │   Grafana   │    │    Loki     │    │   Jaeger    │  │
│  │  (Metrics)  │◀──▶│ (Dashboard) │◀──▶│   (Logs)    │    │  (Traces)   │  │
│  └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘  │
│         │                  │                  │                  │          │
│         │                  │                  │                  │          │
│         ▼                  │                  │                  ▼          │
│  ┌─────────────┐           │           ┌─────────────┐    ┌─────────────┐  │
│  │  Exporters   │           │           │  Logback    │    │  OpenTelemetry│ │
│  │ (Postgres,   │           │           │ (Structured │    │  (Traces)   │  │
│  │   Redis)     │           │           │   Logs)     │    └─────────────┘  │
│  └─────────────┘           │           └─────────────┘                     │
│         │                  │                  │                             │
│         │                  │                  ▼                             │
│         │                  │           ┌─────────────┐                      │
│         │                  │           │    Tempo    │                      │
│         │                  │           │  (Traces)   │                      │
│         │                  │           └─────────────┘                      │
│         │                  │                  │                             │
│         ▼                  ▼                  ▼                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        Application Services                          │   │
│  │  (Gateway, Keycloak, Kafka, MinIO, PostgreSQL, Redis)                │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Components

| Component | Purpose | Port | Image |
|-----------|---------|------|-------|
| **Prometheus** | Metrics collection and alerting | 9090 | `prom/prometheus:latest` |
| **Grafana** | Visualization and dashboards | 3000 | `grafana/grafana:latest` |
| **Jaeger** | Distributed tracing UI | 16686 | `jaegertracing/all-in-one:latest` |
| **Tempo** | Distributed tracing backend | 3200, 4317, 4318 | `grafana/tempo:latest` |
| **Loki** | Log aggregation | 3100 | `grafana/loki:latest` |
| **Alertmanager** | Alert routing and grouping | 9093 | `prom/alertmanager:latest` |
| **Postgres Exporter** | PostgreSQL metrics | 9187 | `prometheuscommunity/postgres-exporter:latest` |
| **Redis Exporter** | Redis metrics | 9121 | `oliver006/redis_exporter:latest` |

## Component Details

### Prometheus

**Ownership:** Platform Team — core metrics infrastructure.

**Task:** Scrape metrics from all application and infrastructure services, store time-series data, evaluate alerting rules, and provide a query API (PromQL) for Grafana dashboards.

**Use Cases:**
- Monitor service health (up/down status)
- Track JVM metrics (memory, GC, threads)
- Monitor database connections and query performance
- Track Kafka consumer lag and throughput
- Alert on high error rates and latency

**Pros:**
- Open source with large ecosystem
- Powerful PromQL query language
- Pull-based model — services don't need to push data
- Native integration with Grafana
- Flexible alerting with Alertmanager

**Cons:**
- Not designed for long-term storage (use Thanos/Cortex for that)
- High cardinality can cause performance issues
- No built-in authentication (use reverse proxy)
- Local storage is not clustered

---

### Grafana

**Ownership:** Platform Team — visualization layer.

**Task:** Provide a unified dashboard for metrics (Prometheus), logs (Loki), and traces (Tempo/Jaeger). Enable correlation across the three pillars of observability.

**Use Cases:**
- Visualize service SLIs/SLOs
- Correlate metrics spikes with traces and logs
- Create custom business dashboards
- Share dashboards across teams

**Pros:**
- Supports dozens of data sources
- Excellent trace-log-metrics correlation
- Dashboard as code (JSON provisioning)
- Alerting (though Prometheus/Alertmanager is preferred)
- Large template and plugin ecosystem

**Cons:**
- Can become slow with hundreds of dashboards
- Alerting is less mature than Prometheus/Alertmanager
- Requires careful access control in multi-tenant environments

---

### Loki

**Ownership:** Platform Team — log aggregation.

**Task:** Collect, index, and query structured logs from all services. Designed to be cost-effective and easy to operate compared to full-text search engines like Elasticsearch.

**Use Cases:**
- Search logs by service, level, trace ID, or label
- Correlate logs with metrics and traces in Grafana
- Debug production issues using structured JSON logs
- Retain logs for compliance and audit

**Pros:**
- Lightweight — uses the same storage engine as Prometheus (TSDB)
- Cheap to operate — no full-text indexing
- Native Grafana integration
- LogQL query language similar to PromQL
- Supports multi-tenant log streams

**Cons:**
- Not a full-text search engine — limited grep capabilities
- Higher latency than Elasticsearch for complex queries
- Less mature ecosystem
- No built-in log parsing (relies on structured logs from applications)

**When to use Loki:** When you need log aggregation with Grafana integration and want to avoid the operational overhead of Elasticsearch. Best for teams already using Prometheus.

**When NOT to use Loki:** When you need full-text search across logs, complex log parsing pipelines, or have existing Elasticsearch/OpenSearch investments.

---

### Tempo

**Ownership:** Platform Team — distributed tracing backend.

**Task:** Store and query distributed traces. Designed for massive scale — can store traces in object storage (S3, GCS, Azure Blob) at low cost.

**Use Cases:**
- Store traces from OpenTelemetry-instrumented services
- Query traces by service, operation, duration, or tags
- Correlate traces with metrics and logs in Grafana
- Support high-cardinality trace storage

**Pros:**
- Extremely cost-effective — stores traces in object storage
- Scales to billions of traces
- Native Grafana integration with service map
- Supports OTLP natively
- No dependency on a separate database

**Cons:**
- Newer project — smaller community than Jaeger
- Query performance depends on object storage latency
- Less UI feature-rich than Jaeger UI
- Trace search is less flexible than Jaeger

**When to use Tempo:** When you need massive scale, cost-effective trace storage, and tight Grafana integration. Best for cloud-native environments with object storage.

**When NOT to use Tempo:** When you need advanced trace analysis features, have a small deployment where cost isn't a concern, or need the richer Jaeger UI.

---

### Jaeger

**Ownership:** Platform Team — distributed tracing (alternative to Tempo).

**Task:** Provide distributed tracing collection, storage, and visualization. The all-in-one image includes agent, collector, query, and UI in a single container for development.

**Use Cases:**
- Visualize request flow across microservices
- Identify performance bottlenecks in distributed systems
- Debug errors with full context propagation
- Analyze service dependencies

**Pros:**
- Mature project with large community
- Rich UI with deep trace analysis features
- Supports multiple storage backends (Cassandra, Elasticsearch, Kafka, memory)
- Well-documented with many client libraries
- Can run in all-in-one mode for development

**Cons:**
- More resource-intensive than Tempo
- Elasticsearch/Cassandra dependencies for production add operational overhead
- Higher storage cost for long-term retention
- Less native Grafana integration than Tempo

**When to use Jaeger:** When you need advanced trace analysis, have existing Elasticsearch infrastructure, or prefer the Jaeger UI for deep-dive investigations.

**When NOT to use Jaeger:** When you need massive scale with low cost, prefer Grafana-native trace visualization, or want to avoid additional database dependencies.

---

### Alertmanager

**Ownership:** Platform Team — alert routing.

**Task:** Receive alerts from Prometheus, deduplicate, group, and route them to notification channels (email, Slack, PagerDuty, etc.).

**Use Cases:**
- Group related alerts to reduce noise
- Route alerts to different teams based on labels
- Suppress alerts during maintenance windows
- Send notifications via multiple channels

**Pros:**
- Built-in deduplication and grouping
- Flexible routing based on labels
- Supports inhibition and silencing
- Webhook integration for custom notification systems

**Cons:**
- No built-in notification channels — requires integration
- Can be complex to configure for large teams
- No built-in on-call scheduling (use external tools)

---

### Postgres Exporter

**Ownership:** Database Team — PostgreSQL metrics.

**Task:** Expose PostgreSQL database metrics in Prometheus format by querying `pg_stat_*` views and `pg_settings`.

**Use Cases:**
- Monitor connection pool utilization
- Track database size and growth
- Monitor query performance and slow queries
- Alert on high connection counts

**Pros:**
- Zero configuration — auto-discovers databases
- Exposes 100+ PostgreSQL metrics
- Lightweight — single binary
- Supports custom queries

**Cons:**
- Requires database access permissions
- Can add load to the database if scrape interval is too short
- Some metrics require PostgreSQL extensions

---

### Redis Exporter

**Ownership:** Platform Team — Redis metrics.

**Task:** Expose Redis instance metrics in Prometheus format by using `INFO` and `CLIENT LIST` commands.

**Use Cases:**
- Monitor memory usage and eviction policies
- Track connected clients and command throughput
- Monitor keyspace hits/misses
- Alert on high memory or connection counts

**Pros:**
- Zero configuration
- Exposes 50+ Redis metrics
- Supports Redis Cluster and Sentinel
- Lightweight

**Cons:**
- Requires Redis AUTH password
- `INFO` command can be expensive on large instances
- Some metrics require Redis 6.0+

## Quick Start

### Start the Monitoring Stack

```bash
# Start infrastructure + monitoring
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.monitoring.yml --profile infrastructure --profile monitoring up -d

# Verify all services are running
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.monitoring.yml --profile infrastructure --profile monitoring ps
```

### Access the UIs

| Service | URL | Default Credentials |
|---------|-----|---------------------|
| Grafana | http://localhost:3000 | admin / admin (or `GRAFANA_ADMIN_PASSWORD`) |
| Prometheus | http://localhost:9090 | — |
| Jaeger UI | http://localhost:16686 | — |
| Tempo Query | http://localhost:3200 | — |
| Loki | http://localhost:3100 | — |
| Alertmanager | http://localhost:9093 | — |

## Configuration

### Prometheus

**Configuration file:** [`infrastructure/docker/prometheus/prometheus.yml`](infrastructure/docker/prometheus/prometheus.yml)

**Alerting rules:** [`infrastructure/docker/prometheus/alerting_rules.yml`](infrastructure/docker/prometheus/alerting_rules.yml)

#### Scrape Targets

| Job Name | Target | Metrics Path | Interval |
|----------|--------|--------------|----------|
| `prometheus` | `localhost:9090` | `/metrics` | 15s |
| `gateway` | `gateway:8080` | `/actuator/prometheus` | 10s |
| `keycloak` | `keycloak:8080` | `/realms/erpai/metrics` | 30s |
| `postgres` | `postgres-exporter:9187` | `/metrics` | 15s |
| `redis` | `redis-exporter:9121` | `/metrics` | 15s |
| `kafka` | `kafka:9308` | `/metrics` | 15s |
| `minio` | `minio:9000` | `/minio/v2/metrics/cluster` | 30s |
| `tempo` | `tempo:3200` | `/metrics` | 15s |
| `loki` | `loki:3100` | `/metrics` | 15s |
| `jaeger` | `jaeger:14269` | `/metrics` | 15s |

#### Practical Examples

**Query all services status:**
```promql
up
```

**Query PostgreSQL active connections:**
```promql
pg_stat_activity_count
```

**Query Redis memory usage:**
```promql
redis_memory_used_bytes / redis_memory_max_bytes * 100
```

**Query Kafka consumer lag:**
```promql
kafka_consumergroup_lag
```

**Query JVM memory usage:**
```promql
jvm_memory_used_bytes{area="heap"}
```

### Grafana

**Datasources:** [`infrastructure/docker/grafana/provisioning/datasources/prometheus.yml`](infrastructure/docker/grafana/provisioning/datasources/prometheus.yml)

**Dashboards:** [`infrastructure/docker/grafana/provisioning/dashboards/`](infrastructure/docker/grafana/provisioning/dashboards/)

#### Provisioned Dashboards

| Dashboard | Description |
|-----------|-------------|
| **Infrastructure Overview** | Service status, PostgreSQL connections, Redis memory, Kafka consumer lag |
| **JVM Metrics** | JVM memory (heap/non-heap), GC pause duration, thread count, CPU usage |
| **Application Metrics** | Request rate, error rate, P95/P99 latency, active connections |
| **Database Metrics** | PostgreSQL connections, database size, transaction rate, cache hit ratio |
| **Kafka Metrics** | Messages in/out per second, bytes in/out, consumer lag, partition count |
| **Distributed Traces** | Trace overview from Tempo/Jaeger |
| **Logs** | Log stream from Loki |

#### Practical Examples

**View service health:**
1. Open Grafana → Dashboards → Infrastructure Overview
2. Check the "Service Status" panel — green = up, red = down

**Investigate high error rate:**
1. Open Grafana → Dashboards → Application Metrics
2. Check "Error Rate" panel for spike
3. Click on the spike to filter by time range
4. Navigate to Jaeger/Tempo traces for that time period
5. Correlate with logs in Loki

**Check database performance:**
1. Open Grafana → Dashboards → Database Metrics
2. Review "PostgreSQL Connections" — should be below 80% of max
3. Check "Cache Hit Ratio" — should be > 99%
4. Review "Transaction Rate" for commits vs rollbacks

### Jaeger

**Configuration:** Service is defined in [`compose.monitoring.yml`](compose.monitoring.yml)

#### Practical Examples

**Search for traces:**
```bash
# Search for traces from a specific service
curl "http://localhost:16686/api/traces?service=finance-service&limit=20"

# Search for traces with errors
curl "http://localhost:16686/api/traces?service=finance-service&tags=error:true&limit=20"

# Get a specific trace by ID
curl "http://localhost:16686/api/traces/{trace-id}"
```

**UI Workflow:**
1. Open http://localhost:16686
2. Select a service from the dropdown (e.g., `finance-service`)
3. Set a time range
4. Click "Search Traces"
5. Click on a trace to view spans
6. Click on a span to see tags, logs, and process info

### Tempo

**Configuration:** [`infrastructure/docker/tempo/tempo.yml`](infrastructure/docker/tempo/tempo.yml)

#### Practical Examples

**Query traces via Tempo API:**
```bash
# Search for traces
curl "http://localhost:3200/api/search?service=finance-service&limit=20"

# Get a specific trace
curl "http://localhost:3200/api/traces/{trace-id}"
```

**Send traces via OTLP:**
```bash
# gRPC endpoint
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317

# HTTP endpoint
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318
```

### Loki

**Configuration:** [`infrastructure/docker/loki/local-config.yaml`](infrastructure/docker/loki/local-config.yaml)

#### Practical Examples

**Query logs via Loki API:**
```bash
# Query all logs from a service
curl "http://localhost:3100/loki/api/v1/query_range?query={service=\"finance-service\"}&limit=100"

# Query logs with a specific log level
curl "http://localhost:3100/loki/api/v1/query_range?query={service=\"finance-service\",level=\"ERROR\"}&limit=100"

# Query logs with a specific trace ID
curl "http://localhost:3100/loki/api/v1/query_range?query={traceId=\"abc123\"}&limit=100"
```

**UI Workflow (Grafana Explore):**
1. Open Grafana → Explore
2. Select "Loki" as datasource
3. Enter a LogQL query:
   ```
   {service="finance-service"} |= "error"
   ```
4. Click "Run query" to see results
5. Click on a log line to see surrounding context

## Alerting

### Configured Alerts

| Alert | Condition | Severity |
|-------|-----------|----------|
| `ServiceDown` | Any service down for 1m | Critical |
| `PostgresDown` | PostgreSQL down for 1m | Critical |
| `RedisDown` | Redis down for 1m | Critical |
| `KafkaDown` | Kafka down for 1m | Critical |
| `KeycloakDown` | Keycloak down for 1m | Critical |
| `MinioDown` | MinIO down for 1m | Critical |
| `HighMemoryUsage` | Memory > 90% for 5m | Warning |
| `HighCPUUsage` | CPU > 80% for 5m | Warning |
| `HighErrorRate` | Error rate > 1% for 5m | Critical |
| `HighLatency` | P99 latency > 2s for 5m | Warning |
| `PostgresHighConnections` | Connection usage > 80% for 5m | Warning |
| `PostgresSlowQueries` | > 100 active queries for 5m | Warning |
| `KafkaHighConsumerLag` | Consumer lag > 10,000 for 5m | Warning |
| `KafkaOfflinePartitions` | Any partition offline for 1m | Critical |
| `RedisHighMemoryUsage` | Redis memory > 90% for 5m | Warning |
| `RedisHighConnections` | > 1000 clients for 5m | Warning |

### Alertmanager Configuration

Alertmanager is included but uses default configuration. To customize alert routing:

1. Create [`infrastructure/docker/alertmanager/alertmanager.yml`](infrastructure/docker/alertmanager/alertmanager.yml)
2. Mount it in [`compose.monitoring.yml`](compose.monitoring.yml):
   ```yaml
   alertmanager:
     volumes:
       - ./infrastructure/docker/alertmanager/alertmanager.yml:/etc/alertmanager/alertmanager.yml:ro
   ```

## Troubleshooting

### Prometheus Not Scraping Targets

**Symptom:** Targets show as "DOWN" in Prometheus UI.

**Troubleshooting steps:**
1. Check if the target service is running:
   ```bash
   docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.monitoring.yml --profile infrastructure --profile monitoring ps
   ```
2. Verify the target is reachable from the Prometheus container:
   ```bash
   docker exec erpai-prometheus wget -qO- http://postgres-exporter:9187/metrics
   ```
3. Check Prometheus logs:
   ```bash
   docker logs erpai-prometheus
   ```
4. Verify the scrape config in `prometheus.yml` matches the service name and port.

### Grafana Dashboards Not Loading

**Symptom:** Dashboards show "No data" or fail to load.

**Troubleshooting steps:**
1. Verify Prometheus datasource is configured:
   - Go to http://localhost:3000 → Connections → Data Sources → Prometheus
   - Click "Test" to verify connectivity
2. Check if dashboard JSON files are mounted correctly:
   ```bash
   docker exec erpai-grafana ls /etc/grafana/provisioning/dashboards/
   ```
3. Check Grafana logs:
   ```bash
   docker logs erpai-grafana
   ```
4. Verify the dashboard provisioning config:
   ```bash
   docker exec erpai-grafana cat /etc/grafana/provisioning/dashboards/dashboards.yml
   ```

### Jaeger Traces Not Appearing

**Symptom:** Jaeger UI shows no traces.

**Troubleshooting steps:**
1. Verify Jaeger is running:
   ```bash
   curl http://localhost:16686/
   ```
2. Check if traces are being sent:
   ```bash
   # Check Jaeger logs
   docker logs erpai-jaeger
   ```
3. Verify OTLP endpoints are accessible:
   ```bash
   # gRPC
   curl -v telnet://localhost:4317

   # HTTP
   curl -v telnet://localhost:4318
   ```
4. Check Tempo (if using Tempo as backend):
   ```bash
   curl http://localhost:3200/ready
   ```

### Loki Logs Not Appearing

**Symptom:** Loki returns empty results or errors.

**Troubleshooting steps:**
1. Verify Loki is running:
   ```bash
   curl http://localhost:3100/ready
   ```
2. Check Loki logs:
   ```bash
   docker logs erpai-loki
   ```
3. Test a simple query:
   ```bash
   curl "http://localhost:3100/loki/api/v1/query_range?query={service=~\".*\"}&limit=10"
   ```
4. Verify the application is sending logs in the expected format (JSON with `service` field).

### Alertmanager Not Sending Alerts

**Symptom:** Alerts fire in Prometheus but no notifications are sent.

**Troubleshooting steps:**
1. Verify Alertmanager is running:
   ```bash
   curl http://localhost:9093/
   ```
2. Check Alertmanager configuration:
   ```bash
   docker exec erpai-alertmanager cat /etc/alertmanager/alertmanager.yml
   ```
3. Check Alertmanager logs:
   ```bash
   docker logs erpai-alertmanager
   ```
4. Verify Prometheus has Alertmanager configured:
   ```bash
   curl http://localhost:9090/api/v1/alertmanagers
   ```

### High Memory Usage on Monitoring Services

**Symptom:** Monitoring containers use excessive memory.

**Troubleshooting steps:**
1. Check container resource usage:
   ```bash
   docker stats erpai-prometheus erpai-grafana erpai-tempo erpai-loki erpai-jaeger
   ```
2. Adjust resource limits in [`compose.monitoring.yml`](compose.monitoring.yml):
   ```yaml
   deploy:
     resources:
       limits:
         memory: 512M  # Reduce from 1G
         cpus: '0.25'   # Reduce from 0.5
   ```
3. For Prometheus, reduce retention:
   ```yaml
   command:
     - '--storage.tsdb.retention.time=7d'  # Reduce from 30d
   ```
4. For Loki, reduce chunk retention in [`local-config.yaml`](infrastructure/docker/loki/local-config.yaml):
   ```yaml
   schema_config:
     configs:
       - from: 2024-01-01
         store: tsdb
         object_store: filesystem
         schema: v13
         index:
           prefix: index_
           period: 24h  # Reduce from 24h to 12h or less
   ```

### Data Not Persisting Between Restarts

**Symptom:** Metrics, logs, or traces are lost after container restart.

**Troubleshooting steps:**
1. Verify volumes are created:
   ```bash
   docker volume ls | grep erpai
   ```
2. Check volume mounts in [`compose.monitoring.yml`](compose.monitoring.yml):
   ```yaml
   volumes:
     - prometheus_data:/prometheus
     - grafana_data:/var/lib/grafana
     - tempo_data:/tmp/tempo
     - loki_data:/loki
     - jaeger_data:/data
   ```
3. Ensure volumes are not being removed:
   ```bash
   # Do NOT use --volumes flag when stopping
   docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.monitoring.yml --profile infrastructure --profile monitoring down
   ```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `GRAFANA_ADMIN_PASSWORD` | `admin` | Grafana admin password |
| `POSTGRES_PASSWORD` | `erpai_dev_password` | PostgreSQL password (used by exporter) |
| `REDIS_PASSWORD` | `erpai_dev_password` | Redis password (used by exporter) |

## Resource Requirements

Minimum resources for the monitoring stack:

| Service | CPU | Memory |
|---------|-----|--------|
| Prometheus | 0.25 | 512M |
| Grafana | 0.25 | 256M |
| Tempo | 0.25 | 512M |
| Jaeger | 0.25 | 256M |
| Loki | 0.25 | 256M |
| Alertmanager | 0.1 | 128M |
| Postgres Exporter | 0.1 | 128M |
| Redis Exporter | 0.1 | 128M |
| **Total** | **1.5** | **2.5G** |

## Backup and Retention

### Prometheus Data
- Retention: 30 days (configurable via `--storage.tsdb.retention.time`)
- Backup: Copy `prometheus_data` volume
  ```bash
  docker run --rm -v erpai-platform_prometheus_data:/data -v $(pwd):/backup alpine tar czf /backup/prometheus-backup.tar.gz /data
  ```

### Grafana Dashboards
- Dashboards are provisioned from files in [`infrastructure/docker/grafana/provisioning/dashboards/`](infrastructure/docker/grafana/provisioning/dashboards/)
- To backup custom dashboards: Export via Grafana UI or copy `grafana_data` volume

### Loki Logs
- Retention: 168 hours (7 days) for old samples
- Backup: Copy `loki_data` volume

### Tempo Traces
- Retention: 720 hours (30 days)
- Backup: Copy `tempo_data` volume

### Jaeger Traces
- Retention: Depends on storage backend (default: in-memory/ephemeral)
- For production, configure persistent storage in `compose.monitoring.yml`

## Production Considerations

1. **Authentication:** Enable authentication for Prometheus, Grafana, and Jaeger
2. **TLS:** Configure TLS for all endpoints
3. **Persistence:** Use persistent volumes for all data
4. **Replication:** Run multiple Prometheus instances for high availability
5. **Long-term storage:** Configure Prometheus remote write to Thanos or Cortex
6. **Alert routing:** Configure Alertmanager with real notification channels (email, Slack, PagerDuty)
7. **Resource limits:** Set appropriate CPU/memory limits based on load
8. **Security:** Run containers as non-root users, use read-only filesystems where possible
