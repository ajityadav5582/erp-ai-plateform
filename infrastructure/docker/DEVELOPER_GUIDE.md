# Developer Guide — Monitoring & Observability

This guide helps Java (Spring Boot) and React developers instrument their applications for the ERP AI Platform monitoring stack.

## Table of Contents

1. [Overview](#overview)
2. [Java (Spring Boot) Developer Guide](#java-spring-boot-developer-guide)
3. [React Developer Guide](#react-developer-guide)
4. [Common Tasks](#common-tasks)
5. [Troubleshooting](#troubleshooting)

---

## Overview

The ERP AI Platform uses the **three pillars of observability**:

| Pillar | Technology | Storage | Visualization |
|--------|-----------|---------|---------------|
| **Metrics** | Micrometer + Prometheus | Prometheus | Grafana |
| **Logs** | Logback + Structured JSON | Loki | Grafana |
| **Traces** | OpenTelemetry | Tempo / Jaeger | Grafana / Jaeger UI |

### Data Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Metrics   │────▶│  Prometheus │────▶│   Grafana   │
│ (Micrometer)│     │             │     │             │
└─────────────┘     └─────────────┘     └─────────────┘
                                                  │
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│    Logs     │────▶│     Loki    │────▶│             │
│  (Logback)  │     │             │     │             │
└─────────────┘     └─────────────┘     │             │
                                                  │
┌─────────────┐     ┌─────────────┐     │             │
│   Traces    │────▶│    Tempo    │────▶│             │
│ (OpenTelemetry)│   │             │     │             │
└─────────────┘     └─────────────┘     └─────────────┘
```

---

## Java (Spring Boot) Developer Guide

### Prerequisites

- Spring Boot 3.x
- Java 17+
- Maven or Gradle

### 1. Add Dependencies

#### Maven (`pom.xml`)

```xml
<dependencies>
    <!-- Spring Boot Actuator for metrics and health -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Prometheus metrics -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>

    <!-- OpenTelemetry API -->
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-api</artifactId>
        <version>1.32.0</version>
    </dependency>

    <!-- OpenTelemetry SDK -->
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-sdk</artifactId>
        <version>1.32.0</version>
    </dependency>

    <!-- OpenTelemetry exporter OTLP -->
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-exporter-otlp</artifactId>
        <version>1.32.0</version>
    </dependency>

    <!-- OpenTelemetry instrumentation for Spring Boot -->
    <dependency>
        <groupId>io.opentelemetry.instrumentation</groupId>
        <artifactId>opentelemetry-spring-boot-starter</artifactId>
        <version>1.32.0</version>
    </dependency>

    <!-- Logback for structured logging -->
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
    </dependency>
</dependencies>
```

#### Gradle (`build.gradle`)

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-registry-prometheus'
    implementation 'io.opentelemetry:opentelemetry-api:1.32.0'
    implementation 'io.opentelemetry:opentelemetry-sdk:1.32.0'
    implementation 'io.opentelemetry:opentelemetry-exporter-otlp:1.32.0'
    implementation 'io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter:1.32.0'
    implementation 'ch.qos.logback:logback-classic'
}
```

### 2. Configure `application.yml`

```yaml
spring:
  application:
    name: finance-service  # Must be unique per service

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: always
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      environment: development

# OpenTelemetry Configuration
otel:
  service:
    name: ${spring.application.name}
  traces:
    exporter: otlp
    endpoint: http://tempo:4317  # or http://jaeger:4317
  metrics:
    exporter: none  # We use Micrometer for metrics
  resource:
    attributes:
      service.name: ${spring.application.name}
      service.version: 1.0.0
      deployment.environment: development
```

### 3. Enable Actuator and Metrics

```java
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.JvmMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;

@Configuration
public class MonitoringConfig {

    @Bean
    public JvmMetrics jvmMetrics(MeterRegistry registry) {
        return new JvmMetrics(registry);
    }

    @Bean
    public ProcessorMetrics processorMetrics(MeterRegistry registry) {
        return new ProcessorMetrics(registry);
    }
}
```

### 4. Add Custom Business Metrics

```java
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class InvoiceMetrics {

    private final Counter invoicesCreated;
    private final Counter invoicesPaid;
    private final Timer invoiceProcessingTime;

    public InvoiceMetrics(MeterRegistry registry) {
        this.invoicesCreated = Counter.builder("erpai.finance.invoices.created")
                .description("Total number of invoices created")
                .register(registry);

        this.invoicesPaid = Counter.builder("erpai.finance.invoices.paid")
                .description("Total number of invoices paid")
                .register(registry);

        this.invoiceProcessingTime = Timer.builder("erpai.finance.invoice.processing.time")
                .description("Time taken to process an invoice")
                .register(registry);
    }

    public void recordInvoiceCreated() {
        invoicesCreated.increment();
    }

    public void recordInvoicePaid() {
        invoicesPaid.increment();
    }

    public void recordProcessingTime(Runnable task) {
        invoiceProcessingTime.record(task);
    }
}
```

**Usage in service:**

```java
@Service
public class InvoiceService {

    private final InvoiceMetrics metrics;

    public InvoiceService(InvoiceMetrics metrics) {
        this.metrics = metrics;
    }

    public Invoice createInvoice(CreateInvoiceRequest request) {
        return metrics.recordProcessingTime(() -> {
            // Business logic here
            metrics.recordInvoiceCreated();
            return invoiceRepository.save(invoice);
        });
    }
}
```

### 5. Add Distributed Tracing

```java
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final Tracer tracer = GlobalOpenTelemetry.getTracer("payment-service");

    public Payment processPayment(PaymentRequest request) {
        Span span = tracer.spanBuilder("processPayment")
                .setAttribute("payment.method", request.method())
                .setAttribute("payment.amount", request.amount())
                .startSpan();

        try (var scope = span.makeCurrent()) {
            // Business logic
            Payment payment = paymentRepository.save(createPayment(request));
            span.setAttribute("payment.id", payment.id());
            span.setAttribute("payment.status", payment.status());
            return payment;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

### 6. Configure Structured Logging

#### `logback-spring.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <fieldNames>
                <timestamp>timestamp</timestamp>
                <level>level</level>
                <logger>logger</logger>
                <message>message</message>
                <thread>thread</thread>
            </fieldNames>
            <includeContext>false</includeContext>
            <includeMdcKeyName>true</includeMdcKeyName>
            <mdcKeys>traceId,spanId,tenantId,userId,requestId</mdcKeys>
        </encoder>
    </appender>

    <appender name="ASYNC_CONSOLE" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="CONSOLE"/>
        <queueSize>512</queueSize>
        <discardingThreshold>0</discardingThreshold>
    </appender>

    <root level="INFO">
        <appender-ref ref="ASYNC_CONSOLE"/>
    </root>

    <logger name="com.erpai" level="DEBUG"/>
    <logger name="org.springframework" level="INFO"/>
    <logger name="org.hibernate" level="WARN"/>
</configuration>
```

#### Add MDC Context Filter

```java
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

public class MdcFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            MDC.put("requestId", UUID.randomUUID().toString());
            MDC.put("traceId", getTraceId());
            MDC.put("spanId", getSpanId());
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String getTraceId() {
        // Get from OpenTelemetry context
        return io.opentelemetry.api.trace.Span.current().getSpanContext().getTraceId();
    }

    private String getSpanId() {
        return io.opentelemetry.api.trace.Span.current().getSpanContext().getSpanId();
    }
}
```

#### Register the Filter

```java
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {

    @Bean
    public FilterRegistrationBean<MdcFilter> mdcFilter() {
        FilterRegistrationBean<MdcFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new MdcFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }
}
```

### 7. Verify Metrics Endpoint

Start your service and verify:

```bash
# Check health
curl http://localhost:8080/actuator/health

# Check metrics
curl http://localhost:8080/actuator/prometheus | head -50

# Check if your custom metrics are present
curl http://localhost:8080/actuator/prometheus | grep erpai_finance
```

### 8. Verify Traces

1. Start the monitoring stack
2. Make a request to your service
3. Open Jaeger UI: http://localhost:16686
4. Search for traces by service name
5. Click on a trace to see spans

### 9. Verify Logs

1. Make a request to your service
2. Open Grafana → Explore → Loki
3. Query: `{service="finance-service"}`
4. You should see structured JSON logs with `traceId` and `spanId`

---

## React Developer Guide

### Prerequisites

- React 18+
- Node.js 18+

### 1. Add Dependencies

```bash
npm install @opentelemetry/api @opentelemetry/sdk-trace-web @opentelemetry/exporter-trace-otlp-http
```

### 2. Initialize OpenTelemetry

Create `src/monitoring/telemetry.ts`:

```typescript
import { WebTracerProvider } from '@opentelemetry/sdk-trace-web';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-http';
import { Resource } from '@opentelemetry/resources';
import { SemanticResourceAttributes } from '@opentelemetry/semantic-conventions';
import { BatchSpanProcessor } from '@opentelemetry/sdk-trace-base';

const provider = new WebTracerProvider({
  resource: new Resource({
    [SemanticResourceAttributes.SERVICE_NAME]: 'erpai-web',
    [SemanticResourceAttributes.SERVICE_VERSION]: '1.0.0',
    [SemanticResourceAttributes.DEPLOYMENT_ENVIRONMENT]: 'development',
  }),
});

const exporter = new OTLPTraceExporter({
  url: 'http://localhost:4318/v1/traces',
});

provider.addSpanProcessor(new BatchSpanProcessor(exporter));
provider.register();

export const tracer = provider.getTracer('erpai-web');
```

### 3. Instrument API Calls

Create `src/monitoring/apiInstrumentation.ts`:

```typescript
import { tracer } from './telemetry';

export async function instrumentedFetch(url: string, options?: RequestInit): Promise<Response> {
  const span = tracer.startSpan('http.request', {
    attributes: {
      'http.method': options?.method || 'GET',
      'http.url': url,
    },
  });

  try {
    const response = await fetch(url, options);
    span.setAttribute('http.status_code', response.status);
    span.setStatus({ code: response.ok ? 0 : 1 }); // 0 = OK, 1 = ERROR
    return response;
  } catch (error) {
    span.recordException(error as Error);
    span.setStatus({ code: 1, message: (error as Error).message });
    throw error;
  } finally {
    span.end();
  }
}
```

### 4. Use Instrumented Fetch

```typescript
import { instrumentedFetch } from '../monitoring/apiInstrumentation';

// Instead of fetch, use instrumentedFetch
const response = await instrumentedFetch('/api/finance/invoices', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`,
    'X-Tenant-ID': tenantId,
  },
});
```

### 5. Add Custom Spans

```typescript
import { tracer } from '../monitoring/telemetry';

export function trackPageLoad(pageName: string) {
  const span = tracer.startSpan('page.load', {
    attributes: {
      'page.name': pageName,
    },
  });

  // Track page load time
  const startTime = performance.now();

  return {
    end: () => {
      const duration = performance.now() - startTime;
      span.setAttribute('page.load_time_ms', duration);
      span.end();
    },
  };
}

// Usage
const pageTracker = trackPageLoad('invoice-list');
// ... render page ...
pageTracker.end();
```

### 6. Add User Interaction Tracking

```typescript
import { tracer } from '../monitoring/telemetry';

export function trackButtonClick(buttonName: string, pageName: string) {
  const span = tracer.startSpan('ui.button.click', {
    attributes: {
      'ui.button.name': buttonName,
      'ui.page.name': pageName,
    },
  });
  span.end();
}

// Usage in component
<button onClick={() => trackButtonClick('create-invoice', 'invoice-list')}>
  Create Invoice
</button>
```

### 7. Add Error Tracking

```typescript
import { tracer } from '../monitoring/telemetry';

export function trackError(error: Error, context: Record<string, string>) {
  const span = tracer.startSpan('ui.error', {
    attributes: {
      'error.type': error.name,
      'error.message': error.message,
      ...context,
    },
  });
  span.recordException(error);
  span.setStatus({ code: 1, message: error.message });
  span.end();
}

// Usage
try {
  await submitForm();
} catch (error) {
  trackError(error as Error, {
    'ui.form.name': 'invoice-form',
    'ui.page.name': 'invoice-list',
  });
}
```

### 8. Verify Traces from React

1. Start the monitoring stack
2. Open your React application
3. Perform actions (page loads, button clicks, API calls)
4. Open Jaeger UI: http://localhost:16686
5. Search for service `erpai-web`
6. You should see traces for page loads, API calls, and user interactions

---

## Common Tasks

### Add a New Metric (Java)

1. **Define the metric** in a dedicated metrics class:

```java
@Component
public class OrderMetrics {
    private final Counter ordersCreated;

    public OrderMetrics(MeterRegistry registry) {
        this.ordersCreated = Counter.builder("erpai.sales.orders.created")
                .description("Total orders created")
                .tag("channel", "web")  // Add tags for filtering
                .register(registry);
    }

    public void recordOrderCreated(String channel) {
        ordersCreated.tag("channel", channel).increment();
    }
}
```

2. **Use the metric** in your service:

```java
@Service
public class OrderService {
    private final OrderMetrics metrics;

    public Order createOrder(CreateOrderRequest request) {
        Order order = orderRepository.save(createOrderFromRequest(request));
        metrics.recordOrderCreated("web");
        return order;
    }
}
```

3. **Verify in Prometheus:**

```bash
curl http://localhost:9090/api/v1/query?query=erpai_sales_orders_created
```

### Add a New Trace Span (Java)

```java
import io.opentelemetry.api.GlobalOpenTelemetry;

@Service
public class NotificationService {

    private final Tracer tracer = GlobalOpenTelemetry.getTracer("notification-service");

    public void sendOrderConfirmation(Order order) {
        Span span = tracer.spanBuilder("sendOrderConfirmation")
                .setAttribute("order.id", order.id())
                .setAttribute("order.total", order.total())
                .startSpan();

        try (var scope = span.makeCurrent()) {
            sendEmail(order.customerEmail(), "Order Confirmation", renderTemplate(order));
            span.setAttribute("notification.sent", true);
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

### Add a New Log Pattern (Java)

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    public void logInvoiceCreated(Invoice invoice, String userId) {
        log.info("{\"event\":\"invoice_created\",\"invoiceId\":\"{}\",\"amount\":{},\"userId\":\"{}\"}",
                invoice.id(), invoice.amount(), userId);
    }
}
```

### Query Metrics in Grafana

**Example PromQL queries:**

```promql
# Total requests per second by service
sum(rate(http_server_requests_seconds_count[5m])) by (service)

# Error rate percentage
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) 
/ sum(rate(http_server_requests_seconds_count[5m])) * 100

# P95 latency
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))

# JVM memory usage percentage
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100

# Database connection pool usage
pg_stat_activity_count / pg_settings_max_connections * 100
```

### Query Logs in Grafana (LogQL)

```logql
# All logs from a service
{service="finance-service"}

# Error logs only
{service="finance-service", level="ERROR"}

# Logs with a specific trace ID
{service="finance-service"} |= "trace-id-value"

# Logs containing a specific message
{service="finance-service"} |= "Invoice created"

# JSON parsing
{service="finance-service"} | json | line_format "{{.message}}"
```

### Query Traces in Grafana

1. Go to **Explore** → Select **Tempo** datasource
2. Enter a query:
   ```
   {service="finance-service", span.name="createInvoice"}
   ```
3. Click **Run query**
4. Click on a trace to see the waterfall view

---

## Troubleshooting

### Metrics Not Appearing in Prometheus

**Checklist:**
1. Is the actuator endpoint exposed? (`management.endpoints.web.exposure.include=prometheus`)
2. Is the Prometheus scrape config correct? Check `prometheus.yml`
3. Is the service reachable from the Prometheus container?
   ```bash
   docker exec erpai-prometheus wget -qO- http://finance-service:8080/actuator/prometheus
   ```
4. Check application logs for errors

### Traces Not Appearing in Jaeger/Tempo

**Checklist:**
1. Is OpenTelemetry configured correctly? Check `application.yml`
2. Is the OTLP endpoint reachable?
   ```bash
   curl -v telnet://tempo:4317
   ```
3. Are spans being created and ended? Check for `span.end()` calls
4. Check Jaeger/Tempo logs:
   ```bash
   docker logs erpai-jaeger
   docker logs erpai-tempo
   ```

### Logs Not Appearing in Loki

**Checklist:**
1. Is the log format JSON? Loki requires structured logs
2. Does each log line contain a `service` field?
3. Is the log level appropriate? (INFO, WARN, ERROR)
4. Check Loki logs:
   ```bash
   docker logs erpai-loki
   ```
5. Test a query:
   ```bash
   curl "http://localhost:3100/loki/api/v1/query_range?query={service=~\".*\"}&limit=10"
   ```

### High Cardinality Metrics

**Problem:** Too many unique time series causing Prometheus performance issues.

**Solution:**
- Avoid using high-cardinality labels (e.g., user IDs, request IDs)
- Use bounded labels (e.g., status codes, HTTP methods)
- Aggregate metrics where possible

```java
// BAD - high cardinality
Counter.builder("api.requests")
    .tag("userId", userId)  // Millions of unique values
    .register(registry);

// GOOD - bounded cardinality
Counter.builder("api.requests")
    .tag("method", request.getMethod())  // GET, POST, PUT, DELETE
    .tag("status", String.valueOf(response.getStatus()))  // 200, 404, 500
    .register(registry);
```

---

## Best Practices

### Metrics
- Use **counters** for monotonically increasing values (requests, errors)
- Use **gauges** for point-in-time values (memory, connections)
- Use **histograms** for distributions (latency, payload size)
- Use **timers** for duration measurements (Micrometer combines histogram + timer)
- Keep metric names snake_case and prefixed with domain: `erpai.finance.invoices.created`

### Traces
- Use **span names** that describe the operation: `finance-service.application.createInvoice`
- Add **attributes** for filtering: `order.id`, `customer.id`, `amount`
- Keep spans **short-lived** — end them as soon as the operation completes
- Use **semantic conventions** where possible: `http.method`, `http.status_code`

### Logs
- Use **structured JSON** format
- Include **traceId** and **spanId** in every log line
- Use appropriate **log levels**: ERROR (failures), WARN (degraded), INFO (business events), DEBUG (diagnostics)
- Avoid logging **sensitive data** (passwords, credit card numbers, PII)

---

## Quick Reference

### Service Names

| Service | Service Name (metrics/traces) |
|---------|------------------------------|
| API Gateway | `gateway` |
| Finance Service | `finance-service` |
| HR Service | `hr-service` |
| Inventory Service | `inventory-service` |
| Manufacturing Service | `manufacturing-service` |
| Procurement Service | `procurement-service` |
| Sales Service | `sales-service` |
| AI Service | `ai-service` |
| Web Frontend | `erpai-web` |

### Ports

| Service | Port |
|---------|------|
| Prometheus | 9090 |
| Grafana | 3000 |
| Jaeger UI | 16686 |
| Tempo Query | 3200 |
| Loki | 3100 |
| Alertmanager | 9093 |

### Useful Commands

```bash
# Check all services are running
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.monitoring.yml --profile infrastructure --profile monitoring ps

# View Prometheus targets
curl http://localhost:9090/api/v1/targets

# View Grafana datasources
curl http://localhost:3000/api/datasources

# Search Jaeger traces
curl "http://localhost:16686/api/traces?service=finance-service&limit=20"

# Query Loki logs
curl "http://localhost:3100/loki/api/v1/query_range?query={service=\"finance-service\"}&limit=100"
```
