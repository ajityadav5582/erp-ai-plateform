# Kafka Infrastructure

Apache Kafka is the distributed message broker used for asynchronous communication between microservices in the ERP AI Platform.

## Overview

| Property | Value |
|----------|-------|
| **Image** | `confluentinc/cp-kafka:7.7.0` |
| **Port** | 29092 (external), 9092 (internal) |
| **Container** | `erpai-kafka` |
| **Mode** | KRaft (no ZooKeeper) |
| **Purpose** | Event streaming and message broker |
| **Owner** | Platform Team |

## Architecture

Kafka runs in KRaft mode (Kafka Raft), which eliminates the need for Apache ZooKeeper. The broker acts as both the broker and the controller.

```
┌─────────────────────────────────────────────────────────────────┐
│                      Kafka Cluster                              │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    erpai-kafka                             │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │  │
│  │  │  Broker     │  │ Controller  │  │  Topic       │        │  │
│  │  │  (Port 9092)│  │  (Port 9098)│  │  Partitions  │        │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘        │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ▲                                                               │
│  │                                                               │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  └──│  Producers  │  │  Consumers  │  │   Schema    │          │
│     │ (Services)  │  │ (Services)  │  │  Registry    │          │
│     └─────────────┘  └─────────────┘  └─────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `KAFKA_NODE_ID` | `1` | Broker node ID |
| `KAFKA_CONTROLLER_QUORUM_VOTERS` | `1@kafka:9098` | Controller quorum |
| `KAFKA_PROCESS_ROLES` | `broker,controller` | Roles this node plays |
| `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR` | `1` | Replication for offsets topic |
| `KAFKA_NUM_PARTITIONS` | `12` | Default partitions per topic |
| `KAFKA_AUTO_CREATE_TOPICS_ENABLE` | `true` | Auto-create topics |
| `KAFKA_LOG_RETENTION_HOURS` | `168` | Log retention (7 days) |

### Listeners

| Listener | Port | Purpose |
|----------|------|---------|
| `PLAINTEXT` | 9092 | Internal broker communication |
| `PLAINTEXT_HOST` | 29092 | External client access |
| `CONTROLLER` | 9098 | Controller quorum |

## Topics

### Infrastructure Topics

The following infrastructure topics are created automatically by `kafka-init`:

| Topic | Partitions | Retention | Purpose |
|-------|------------|-----------|---------|
| `dev.system.health.service-up` | 6 | 7 days | Service health events |
| `dev.system.health.service-down` | 6 | 7 days | Service down events |
| `dev.system.metrics` | 6 | 7 days | System metrics |
| `dev.dlq.events` | 6 | 30 days | Failed events (dead letter queue) |
| `dev.dlq.commands` | 6 | 30 days | Failed commands (dead letter queue) |
| `dev.retry.events` | 6 | 1 day | Events for retry |
| `dev.retry.commands` | 6 | 1 day | Commands for retry |
| `dev.audit.events` | 6 | 30 days | Audit trail |

### Topic Naming Convention

Business topics follow the naming standard:

```
{environment}.{domain}.{entity}.{event-type}
```

Examples:
- `dev.finance.invoice.created`
- `dev.hr.employee.onboarded`
- `dev.inventory.stock.updated`

See [`TOPIC_NAMING_STANDARDS.md`](TOPIC_NAMING_STANDARDS.md) for details.

## Relationships

```
┌─────────────────────────────────────────────────────────────────┐
│                         Kafka                                   │
│                                                                 │
│  ▲                                                               │
│  │                                                               │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  └──│  Producers  │  │  Consumers  │  │   Schema    │          │
│     │ (Services)  │  │ (Services)  │  │  Registry    │          │
│     └─────────────┘  └─────────────┘  └─────────────┘          │
│                                                                 │
│  ▲                                                               │
│  │                                                               │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  └──│ Kafka UI    │  │  Exporters  │  │   Init      │          │
│     │ (Management)│  │(Prometheus) │  │  Script     │          │
│     └─────────────┘  └─────────────┘  └─────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

### Related Components

| Component | Relationship |
|-----------|--------------|
| **Schema Registry** | Manages Avro/JSON schemas for topics |
| **Kafka UI** | Web UI for topic management and message browsing |
| **Kafka Init** | Creates infrastructure topics on startup |
| **Microservices** | Produce and consume events |
| **Kafka Exporter** | Scrapes metrics for Prometheus |

## Quick Start

```bash
# Start Kafka
docker compose -f compose.base.yml -f compose.infrastructure.yml up kafka

# Start with Kafka UI
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.development.yml --profile infrastructure up kafka kafka-ui

# Initialize infrastructure topics
docker compose -f compose.base.yml -f compose.infrastructure.yml up kafka-init

# Access Kafka UI
open http://localhost:8082
```

## Kafka UI

Access Kafka UI at http://localhost:8082 to:
- View topics and partitions
- Browse messages
- Monitor consumer groups
- View schema registry
- Create/delete topics

## Producing Messages

### Java (Spring Boot)

```java
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class InvoiceEventProducer {

    private final KafkaTemplate<String, InvoiceCreatedEvent> kafkaTemplate;

    public InvoiceEventProducer(KafkaTemplate<String, InvoiceCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendInvoiceCreated(InvoiceCreatedEvent event) {
        kafkaTemplate.send("dev.finance.invoice.created", event.invoiceId(), event);
    }
}
```

### Configuration

```yaml
spring:
  kafka:
    bootstrap-servers: kafka:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: io.confluent.kafka.serializers.KafkaAvroSerializer
    properties:
      schema.registry.url: http://schema-registry:8081
```

## Consuming Messages

### Java (Spring Boot)

```java
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class InvoiceEventConsumer {

    @KafkaListener(topics = "dev.finance.invoice.created", groupId = "sales-service")
    public void handleInvoiceCreated(InvoiceCreatedEvent event) {
        // Process event
        salesService.updateFromInvoice(event);
    }
}
```

## Monitoring

Kafka metrics are collected via JMX and exposed through the Kafka Exporter:

| Metric | Description |
|--------|-------------|
| `kafka_server_brokertopicmetrics_messagesin_total` | Messages produced per topic |
| `kafka_server_brokertopicmetrics_bytesin_total` | Bytes produced per topic |
| `kafka_consumergroup_lag` | Consumer lag per topic/group |
| `kafka_topic_partitions` | Partition count per topic |
| `kafka_controller_kafkacontroller_activecontrollercount` | Active controller status |

View in Grafana: **Dashboards → Kafka Metrics**

## Retry & Dead Letter Queue

The platform implements a retry strategy with DLQ:

```
Producer → Topic → Consumer (fails) → Retry Topic → Consumer (fails) → DLQ
```

See [`RETRY_STRATEGY.md`](RETRY_STRATEGY.md) and [`DLQ_STRATEGY.md`](DLQ_STRATEGY.md) for details.

## Troubleshooting

### Broker Not Starting

```bash
# Check Kafka logs
docker compose -f compose.base.yml -f compose.infrastructure.yml logs kafka

# Check if ports are available
docker compose -f compose.base.yml -f compose.infrastructure.yml ps kafka
```

### Consumer Lag

Check consumer lag in Kafka UI or via Prometheus:
```promql
kafka_consumergroup_lag
```

If lag is high:
- Increase consumer instances
- Check for slow processing
- Review batch sizes

### Topic Not Found

```bash
# List all topics
docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# Describe a topic
docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --describe --topic dev.finance.invoice.created
```
