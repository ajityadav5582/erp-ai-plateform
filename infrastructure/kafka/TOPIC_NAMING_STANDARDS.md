# Kafka Topic Naming Standards

## 1. Purpose

This document defines the naming conventions and standards for Kafka topics in the ERP AI Platform. These standards ensure consistency, discoverability, and maintainability across all event-driven communication.

## 2. Topic Naming Convention

### 2.1 General Format

```
{environment}.{tenant-context}.{category}.{domain}.{event-type}
```

### 2.2 Components

| Component | Description | Examples |
|-----------|-------------|----------|
| `environment` | Deployment environment | `dev`, `staging`, `prod` |
| `tenant-context` | Multi-tenancy context | `tenant` (for tenant-specific), `system` (for system-wide) |
| `category` | Event category | `events`, `commands`, `queries` |
| `domain` | Business domain | `invoice`, `payment`, `customer`, `inventory` |
| `event-type` | Specific event type | `created`, `updated`, `deleted`, `processed` |

### 2.3 Examples

```
# Domain events
dev.tenant.invoice.invoice-created
dev.tenant.payment.payment-processed
dev.tenant.customer.customer-deactivated

# System events
dev.system.health.service-up
dev.system.metrics

# Dead Letter Queue
dev.dlq.events
dev.dlq.commands

# Retry topics
dev.retry.events
dev.retry.commands

# Audit events
dev.audit.events
```

## 3. Topic Categories

### 3.1 Domain Events

Domain events represent something that happened in a bounded context.

```
{environment}.tenant.{domain}.{event-type}
```

**Examples:**
- `dev.tenant.invoice.invoice-created`
- `dev.tenant.payment.payment-paid`
- `dev.tenant.inventory.stock-adjusted`

### 3.2 Integration Events

Integration events for cross-service communication.

```
{environment}.tenant.{domain}.{event-type}
```

**Examples:**
- `dev.tenant.invoice.invoice-created`
- `dev.tenant.customer.customer-updated`

### 3.3 System Events

Infrastructure and operational events.

```
{environment}.system.{category}.{event-type}
```

**Examples:**
- `dev.system.health.service-up`
- `dev.system.health.service-down`
- `dev.system.metrics`

### 3.4 Dead Letter Queue (DLQ)

Failed events that could not be processed.

```
{environment}.dlq.{category}
```

**Examples:**
- `dev.dlq.events`
- `dev.dlq.commands`

### 3.5 Retry Topics

Events that need retry processing.

```
{environment}.retry.{category}
```

**Examples:**
- `dev.retry.events`
- `dev.retry.commands`

### 3.6 Audit Events

Audit trail for compliance and debugging.

```
{environment}.audit.{category}
```

**Examples:**
- `dev.audit.events`
- `dev.audit.commands`

## 4. Topic Configuration Standards

### 4.1 Default Configuration

| Setting | Value | Description |
|---------|-------|-------------|
| `num.partitions` | 12 | Default partition count |
| `replication.factor` | 1 (dev), 3 (prod) | Replication factor |
| `retention.ms` | 604800000 (7 days) | Message retention |
| `segment.bytes` | 1073741824 (1GB) | Segment size |
| `cleanup.policy` | delete | Cleanup policy |

### 4.2 DLQ Topic Configuration

| Setting | Value | Description |
|---------|-------|-------------|
| `retention.ms` | 2592000000 (30 days) | Extended retention for debugging |
| `cleanup.policy` | delete | Delete after retention |
| `delete.retention.ms` | 86400000 (1 day) | Tombstone retention |

### 4.3 Retry Topic Configuration

| Setting | Value | Description |
|---------|-------|-------------|
| `retention.ms` | 86400000 (1 day) | Short retention |
| `cleanup.policy` | delete | Delete after processing |

## 5. Partition Strategy

### 5.1 Partition Key

Use tenant ID as partition key for ordering guarantees:

```java
// Partition by tenant for ordering
String partitionKey = String.valueOf(tenantId);
```

### 5.2 Partition Count

- **Minimum:** 6 partitions
- **Default:** 12 partitions
- **Maximum:** 100 partitions (per topic)

### 5.3 Scaling

- Scale consumers horizontally within a consumer group
- Each partition can be consumed by only one consumer in a group
- Use partition count that supports expected throughput

## 6. Consumer Group Naming

### 6.1 Format

```
{service-name}.{function}.{instance-id}
```

### 6.2 Examples

```
invoice-service.processor.main
payment-service.handler.worker-1
notification-service.dispatcher.main
```

## 7. No Business Topics Policy

### 7.1 Infrastructure-Only Topics

The following topics are created as infrastructure topics:

- `dev.system.health.service-up`
- `dev.system.health.service-down`
- `dev.system.metrics`
- `dev.dlq.events`
- `dev.dlq.commands`
- `dev.retry.events`
- `dev.retry.commands`
- `dev.audit.events`

### 7.2 Business Topic Creation

Business topics are created dynamically by services using the topic naming convention. Services should:

1. Use the naming convention defined above
2. Create topics on first use (with `auto.create.topics.enable=true` in dev)
3. Configure appropriate retention and partition settings
4. Register schemas in Schema Registry

## 8. Topic Management

### 8.1 Topic Creation

```bash
# Create a topic
kafka-topics.sh --bootstrap-server kafka:9092 \
    --create \
    --topic dev.tenant.invoice.invoice-created \
    --partitions 12 \
    --replication-factor 1 \
    --config retention.ms=604800000
```

### 8.2 Topic Deletion

```bash
# Delete a topic
kafka-topics.sh --bootstrap-server kafka:9092 \
    --delete \
    --topic dev.tenant.invoice.invoice-created
```

### 8.3 Topic Configuration

```bash
# Alter topic configuration
kafka-configs.sh --bootstrap-server kafka:9092 \
    --alter \
    --entity-type topics \
    --entity-name dev.tenant.invoice.invoice-created \
    --add-config retention.ms=1209600000
```

## 9. Monitoring

### 9.1 Key Metrics

- **Consumer lag:** Monitor per consumer group
- **DLQ depth:** Alert when > 100 messages
- **Throughput:** Events per second
- **Error rate:** Failed events percentage

### 9.2 Alerts

- DLQ depth > 100: Alert on-call team
- Consumer lag > 1000: Alert on-call team
- Error rate > 1%: Alert on-call team

## 10. Checklist

- [ ] Topic name follows naming convention
- [ ] Partition count appropriate for throughput
- [ ] Retention period configured correctly
- [ ] Schema registered in Schema Registry
- [ ] Consumer group follows naming convention
- [ ] Monitoring and alerts configured
- [ ] DLQ topic exists for the category
- [ ] Retry topic exists for the category
