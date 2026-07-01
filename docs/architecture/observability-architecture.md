# Observability Architecture

## Overview

The ERP AI Platform implements the three pillars of observability: metrics, logs, and traces.

## Observability Stack

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Observability Stack                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐             │
│  │ Prometheus  │    │   Grafana   │    │    Loki     │             │
│  │  (Metrics)  │◀──▶│ (Dashboard) │◀──▶│   (Logs)    │             │
│  └─────────────┘    └─────────────┘    └─────────────┘             │
│         │                  │                  │                     │
│         │                  │                  │                     │
│         ▼                  │                  ▼                     │
│  ┌─────────────┐           │           ┌─────────────┐             │
│  │ Micrometer  │           │           │   Logback   │             │
│  │  (Metrics)  │           │           │  (Logging)  │             │
│  └─────────────┘           │           └─────────────┘             │
│         │                  │                  │                     │
│         │                  │                  │                     │
│         ▼                  │                  ▼                     │
│  ┌─────────────┐           │           ┌─────────────┐             │
│  │ OpenTelemetry│          │           │  Structured  │             │
│  │  (Traces)   │◀─────────┼──────────▶│    Logs      │             │
│  └─────────────┘           │           └─────────────┘             │
│                             │                                      │
│                             ▼                                      │
│                    ┌─────────────┐                                 │
│                    │    Tempo    │                                 │
│                    │  (Traces)   │                                 │
│                    └─────────────┘                                 │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## Metrics

### Technology: Micrometer + Prometheus

**Metrics Types:**
- Counter: Monotonically increasing values
- Gauge: Point-in-time values
- Histogram: Distribution of values
- Timer: Duration measurements

**Key Metrics:**

| Metric | Type | Description |
|--------|------|-------------|
| `http.server.requests` | Counter | Total HTTP requests |
| `http.server.request.duration` | Timer | Request processing time |
| `jvm.memory.used` | Gauge | JVM memory usage |
| `jvm.gc.pause` | Timer | GC pause duration |
| `process.cpu.usage` | Gauge | CPU usage |
| `kafka.consumer.lag` | Gauge | Consumer lag |
| `db.connection.pool.usage` | Gauge | Connection pool utilization |

### Custom Business Metrics

```java
@Counter("erpai.finance.invoices.created")
void recordInvoiceCreated();

@Timer("erpai.hr.employee.onboarding.duration")
void recordOnboardingDuration(Duration duration);
```

## Logging

### Technology: Logback + Structured Logging

**Log Format (JSON):**
```json
{
  "timestamp": "2024-01-01T00:00:00.000Z",
  "level": "INFO",
  "service": "finance-service",
  "traceId": "abc123",
  "spanId": "def456",
  "tenantId": "tenant-uuid",
  "userId": "user-uuid",
  "message": "Invoice created successfully",
  "invoiceId": "invoice-uuid",
  "amount": 1000.00,
  "mdc": {
    "requestId": "request-uuid"
  }
}
```

**Log Levels:**
- ERROR: Errors requiring immediate attention
- WARN: Potential issues
- INFO: Business events and milestones
- DEBUG: Detailed diagnostic information
- TRACE: Very detailed diagnostic information

## Tracing

### Technology: OpenTelemetry + Tempo

**Trace Context Propagation:**
- HTTP headers: `traceparent`, `tracestate`
- Message headers: Kafka message headers
- Correlation ID: `X-Correlation-ID`

**Span Naming Convention:**
```
<service-name>.<layer>.<operation>
```

Examples:
- `finance-service.application.createInvoice`
- `hr-service.infrastructure.findEmployeeById`

**Key Traces:**
- HTTP request lifecycle
- Database query execution
- Kafka message processing
- External API calls
- AI model inference

## Dashboards

### Standard Dashboards

1. **Service Overview**: Request rate, error rate, latency
2. **JVM Metrics**: Memory, GC, threads
3. **Database**: Connection pool, query performance
4. **Kafka**: Throughput, lag, consumer health
5. **Business Metrics**: Domain-specific KPIs

### Alerting

| Alert | Condition | Severity |
|-------|-----------|----------|
| High Error Rate | Error rate > 1% for 5m | Critical |
| High Latency | P99 latency > 2s for 5m | Warning |
| Low Disk Space | Disk usage > 85% | Critical |
| High Memory | Memory usage > 90% | Warning |
| Consumer Lag | Kafka lag > 10000 | Warning |
| Service Down | Health check failing | Critical |

## Correlation

### Trace-Log Correlation

Every log entry includes trace and span IDs for correlation with distributed traces.

### Metrics-Log Correlation

Metrics include labels that match log fields for cross-referencing.

### Example Investigation Flow

1. Alert fires: High error rate on finance-service
2. Grafana dashboard shows spike in 5xx errors
3. Click through to Tempo traces for the time period
4. Identify slow database queries in trace spans
5. Correlate with database metrics
6. Check logs for error messages with matching trace IDs
