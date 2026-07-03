# Kafka Infrastructure

Apache Kafka message broker configuration for the ERP AI Platform, running in KRaft mode (no Zookeeper).

## Directory Structure

```
infrastructure/kafka/
├── config/
│   └── kraft/
│       └── server.properties    # KRaft mode configuration
├── scripts/
│   ├── healthcheck.sh           # Health check script
│   └── init-topics.sh           # Topic initialization script
├── backups/
│   └── README.md                # Backup documentation
├── TOPIC_NAMING_STANDARDS.md    # Topic naming conventions
├── RETRY_STRATEGY.md            # Retry strategy documentation
├── DLQ_STRATEGY.md              # Dead Letter Queue strategy
└── README.md                    # This file
```

## Quick Start

### Start Kafka

```bash
# Start Kafka with infrastructure services
docker compose -f compose.base.yml -f compose.infrastructure.yml up kafka

# Start with Kafka UI
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.development.yml --profile infrastructure up kafka kafka-ui
```

### Access Kafka UI

Open http://localhost:8082 in your browser to access the Kafka UI.

### Initialize Topics

```bash
# Initialize infrastructure topics
docker compose -f compose.base.yml -f compose.infrastructure.yml up kafka-init
```

## Configuration

### KRaft Mode

Kafka runs in KRaft mode (Kafka Raft Metadata Mode) which eliminates the need for Zookeeper. The configuration is in [`config/kraft/server.properties`](config/kraft/server.properties).

Key settings:
- **Node ID:** 1
- **Process Roles:** broker,controller
- **Listeners:** PLAINTEXT (internal), PLAINTEXT_HOST (external)
- **Partitions:** 12 (default)

### Docker Compose

The Kafka service is defined in [`compose.infrastructure.yml`](../../compose.infrastructure.yml) with:

- **Image:** confluentinc/cp-kafka:7.7.0
- **Ports:** 29092 (external)
- **Health Check:** Broker API versions check
- **Resources:** 2GB memory, 1 CPU

## Services

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| Kafka | confluentinc/cp-kafka:7.7.0 | 29092 | Message broker |
| Schema Registry | confluentinc/cp-schema-registry:7.7.0 | 8081 | Schema management |
| Kafka UI | provectuslabs/kafka-ui:latest | 8082 | Web UI for Kafka |

## Infrastructure Topics

The following infrastructure topics are created automatically:

### System Topics

| Topic | Partitions | Purpose |
|-------|------------|---------|
| `dev.system.health.service-up` | 6 | Service health events |
| `dev.system.health.service-down` | 6 | Service down events |
| `dev.system.metrics` | 6 | System metrics |

### Dead Letter Queue Topics

| Topic | Partitions | Retention | Purpose |
|-------|------------|-----------|---------|
| `dev.dlq.events` | 6 | 30 days | Failed events |
| `dev.dlq.commands` | 6 | 30 days | Failed commands |

### Retry Topics

| Topic | Partitions | Retention | Purpose |
|-------|------------|-----------|---------|
| `dev.retry.events` | 6 | 1 day | Events for retry |
| `dev.retry.commands` | 6 | 1 day | Commands for retry |

### Audit Topics

| Topic | Partitions | Purpose |
|-------|------------|---------|
| `dev.audit.events` | 6 | Audit trail |

## Health Checks

### Docker Health Check

The Kafka container has a built-in health check:

```yaml
healthcheck:
  test: ["CMD-SHELL", "kafka-broker-api-versions.sh --bootstrap-server localhost:9092 || exit 1"]
  interval: 30s
  timeout: 10s
  retries: 10
  start_period: 60s
```

### Manual Health Check

```bash
# Check if Kafka is running
kafka-broker-api-versions.sh --bootstrap-server localhost:9092

# List topics
kafka-topics.sh --bootstrap-server localhost:9092 --list

# Check consumer groups
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
```

## Monitoring

### Key Metrics

- **Consumer lag:** Monitor per consumer group
- **DLQ depth:** Alert when > 100 messages
- **Throughput:** Events per second
- **Error rate:** Failed events percentage

### Prometheus Integration

Add to Prometheus configuration:

```yaml
scrape_configs:
  - job_name: 'kafka'
    static_configs:
      - targets: ['kafka:9092']
```

## Backup and Recovery

See [`backups/README.md`](backups/README.md) for backup procedures.

## Documentation

- [Topic Naming Standards](TOPIC_NAMING_STANDARDS.md)
- [Retry Strategy](RETRY_STRATEGY.md)
- [Dead Letter Queue Strategy](DLQ_STRATEGY.md)
- [Event Standards](../../docs/standards/05-event-standards.md)

## Troubleshooting

### Kafka not starting

```bash
# Check logs
docker compose logs kafka

# Check health status
docker compose ps kafka
```

### Topics not created

```bash
# Run init script manually
docker compose -f compose.base.yml -f compose.infrastructure.yml up kafka-init
```

### Connection refused

```bash
# Check if Kafka is listening
docker compose exec kafka netstat -tlnp | grep 9092

# Check advertised listeners
docker compose exec kafka cat /etc/kafka/kafka.properties
```

## Best Practices

- [ ] Use tenant ID as partition key for ordering
- [ ] Configure appropriate retention for each topic
- [ ] Monitor DLQ depth and consumer lag
- [ ] Use Schema Registry for all events
- [ ] Implement idempotent event handlers
- [ ] Use retry strategy for transient errors
- [ ] Send failed events to DLQ
- [ ] Log all DLQ messages for analysis
