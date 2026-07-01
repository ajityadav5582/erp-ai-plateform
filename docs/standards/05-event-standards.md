# Event Standards

## 1. Purpose

These standards ensure consistency, reliability, and maintainability across all event-driven communication in the ERP SaaS platform. All event design, production, and consumption must follow these guidelines.

## 2. Event-Driven Architecture Principles

- **Loose coupling:** Services communicate via events, not direct calls
- **Eventual consistency:** Accept temporary inconsistency with eventual convergence
- **Idempotency:** Event handlers must be idempotent
- **Schema evolution:** Events must be backward and forward compatible
- **Observability:** All events must be traceable and monitorable

## 3. Event Types

### 3.1 Domain Events

Events that represent something that happened in a domain:

```java
// Event: Invoice was created
public record InvoiceCreatedEvent(
    Long eventId,
    Long tenantId,
    Long invoiceId,
    String invoiceNumber,
    BigDecimal total,
    Instant occurredAt
) implements DomainEvent {}
```

### 3.2 Integration Events

Events for cross-service communication:

```java
// Event: Notify other services about invoice creation
public record InvoiceCreatedIntegrationEvent(
    Long eventId,
    Long tenantId,
    Long invoiceId,
    String invoiceNumber,
    BigDecimal total,
    Instant occurredAt
) implements IntegrationEvent {}
```

### 3.3 System Events

Infrastructure and operational events:

```java
// Event: Health check, metrics, etc.
public record ServiceHealthEvent(
    String serviceName,
    HealthStatus status,
    Instant checkedAt
) implements SystemEvent {}
```

## 4. Event Naming Conventions

### 4.1 Event Names

- **Format:** `{Domain}{PastTenseVerb}Event`
- **Past tense:** Always use past tense (something already happened)
- **Domain prefix:** Include the bounded context
- **Examples:**
  - `InvoiceCreatedEvent`
  - `PaymentProcessedEvent`
  - `CustomerDeactivatedEvent`
  - `InventoryLevelAdjustedEvent`

### 4.2 Topic Naming

```
{environment}.{tenant-context}.{domain}.{event-name}

# Examples
prod.tenant.invoice.invoice-created
prod.tenant.payment.payment-processed
prod.system.health.service-up
```

### 4.3 Consumer Group Naming

```
{service-name}.{function}.{instance-id}

# Examples
invoice-service.processor.1
payment-service.handler.main
notification-service.dispatcher.worker-1
```

## 5. Event Schema Standards

### 5.1 Required Fields

Every event must include:

```java
public interface DomainEvent {
    UUID eventId();           // Unique event identifier
    String eventType();       // Event type name
    Long tenantId();          // Tenant context
    Instant occurredAt();     // When the event occurred
    Instant publishedAt();    // When the event was published
    Integer version();        // Schema version
}
```

### 5.2 Schema Versioning

- **Version field:** Include `version` in every event
- **Backward compatibility:** New version must be readable by old consumers
- **Forward compatibility:** Old version must be readable by new consumers
- **Breaking changes:** Require new topic or event type

```java
// Version 1
public record InvoiceCreatedV1(
    UUID eventId,
    Long tenantId,
    Long invoiceId,
    String invoiceNumber,
    BigDecimal total,
    Instant occurredAt,
    Integer version  // 1
) implements DomainEvent {}

// Version 2 - added currency field (backward compatible)
public record InvoiceCreatedV2(
    UUID eventId,
    Long tenantId,
    Long invoiceId,
    String invoiceNumber,
    BigDecimal total,
    String currency,      // New field
    Instant occurredAt,
    Integer version  // 2
) implements DomainEvent {}
```

### 5.3 Schema Registry

- **Tool:** Confluent Schema Registry or Apicurio Registry
- **Format:** Apache Avro (preferred) or JSON Schema
- **Compatibility:** BACKWARD compatibility enforced
- **Evolution:** Additive changes only (new optional fields)

## 6. Event Production Standards

### 6.1 Event Publisher

```java
@Service
@RequiredArgsConstructor
public class InvoiceEventPublisher {
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final TenantContextHolder tenantContextHolder;
    
    @EventListener
    public void handle(InvoiceCreatedEvent event) {
        DomainEventEnvelope envelope = DomainEventEnvelope.builder()
            .eventId(UUID.randomUUID())
            .eventType(event.eventType())
            .tenantId(tenantContextHolder.getTenantId())
            .payload(event)
            .publishedAt(Instant.now())
            .build();
        
        kafkaTemplate.send("prod.tenant.invoice.invoice-created", 
                          String.valueOf(event.tenantId()), 
                          envelope);
    }
}
```

### 6.2 Production Rules

- **Transactional outbox:** Use transactional outbox pattern for consistency
- **Ordering:** Maintain order within a partition (use tenant ID as key)
- **Retry:** Configure appropriate retry with exponential backoff
- **Dead letter:** Route failed events to DLQ after max retries
- **Idempotency:** Include unique event ID for deduplication

### 6.3 Transactional Outbox

```java
@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxPublisher outboxPublisher;
    
    @Transactional
    public Invoice create(CreateInvoiceCommand command) {
        Invoice invoice = Invoice.create(command);
        Invoice saved = invoiceRepository.save(invoice);
        
        // Save to outbox in same transaction
        outboxEventRepository.save(OutboxEvent.builder()
            .aggregateType("Invoice")
            .aggregateId(saved.getId())
            .eventType("InvoiceCreatedEvent")
            .payload(toJson(saved))
            .build());
        
        return saved;
    }
}
```

## 7. Event Consumption Standards

### 7.1 Event Handler

```java
@Service
@RequiredArgsConstructor
public class InvoiceEventHandler {
    private final PaymentService paymentService;
    private final TenantContextHolder tenantContextHolder;
    
    @KafkaListener(
        topics = "prod.tenant.invoice.invoice-created",
        groupId = "payment-service.handler.main",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handle(DomainEventEnvelope envelope) {
        // Set tenant context
        tenantContextHolder.setTenantId(envelope.tenantId());
        
        // Idempotency check
        if (eventProcessor.isProcessed(envelope.eventId())) {
            return;
        }
        
        // Process event
        switch (envelope.eventType()) {
            case "InvoiceCreatedEvent" -> handleInvoiceCreated(envelope);
            case "InvoicePaidEvent" -> handleInvoicePaid(envelope);
            default -> log.warn("Unknown event type: {}", envelope.eventType());
        }
        
        // Mark as processed
        eventProcessor.markProcessed(envelope.eventId());
    }
}
```

### 7.2 Consumption Rules

- **Idempotency:** Always check if event was already processed
- **Ordering:** Process events in order within a partition
- **Error handling:** Failed events go to DLQ, don't block consumer
- **Tenant context:** Set tenant context from event, not request
- **Dead letter queue:** Monitor and reprocess DLQ events

### 7.3 Consumer Groups

- **One group per service:** Each service has its own consumer group
- **Partitioning:** Use tenant ID as partition key for ordering
- **Scaling:** Scale consumers horizontally within a group
- **Offset management:** Commit offsets after successful processing

## 8. Event Sourcing (Optional)

### 8.1 When to Use

- Audit trail requirements
 Complex state reconstruction
- Temporal queries
- Event replay capabilities

### 8.2 Event Store

```java
@Entity
@Table(name = "events")
public class StoredEvent {
    @Id
    private UUID eventId;
    
    private String aggregateType;
    private Long aggregateId;
    private String eventType;
    private Integer version;
    private String payload;
    private Instant occurredAt;
    private Instant storedAt;
}
```

### 8.3 Aggregate Design

```java
public class InvoiceAggregate {
    private Long id;
    private InvoiceStatus status;
    private BigDecimal total;
    private List<DomainEvent> changes = new ArrayList<>();
    
    public void apply(InvoiceCreatedEvent event) {
        this.id = event.invoiceId();
        this.status = InvoiceStatus.CREATED;
        this.total = event.total();
        this.changes.add(event);
    }
    
    public List<DomainEvent> getUncommittedChanges() {
        return changes;
    }
    
    public void markChangesAsCommitted() {
        changes.clear();
    }
}
```

## 9. Saga Pattern

### 9.1 Choreography

Events trigger subsequent events without central coordination:

```
OrderCreated → ReserveInventory → ProcessPayment → ShipOrder
```

### 9.2 Orchestration

Central coordinator manages the saga:

```java
@Service
public class OrderSagaOrchestrator {
    public void processOrder(Order order) {
        try {
            reserveInventory(order);
            processPayment(order);
            shipOrder(order);
            completeOrder(order);
        } catch (Exception e) {
            compensateOrder(order);
            throw e;
        }
    }
}
```

### 9.3 Compensation

- **Idempotent:** Compensation actions must be idempotent
- **Ordered:** Compensate in reverse order of execution
- **Complete:** All-or-nothing semantics

## 10. Event Monitoring

### 10.1 Metrics

- **Throughput:** Events produced/consumed per second
- **Latency:** Time from production to consumption
- **Error rate:** Failed events per total events
- **DLQ depth:** Number of events in dead letter queue
- **Consumer lag:** Messages behind for each consumer group

### 10.2 Alerts

- **DLQ depth > 100:** Alert on-call team
- **Consumer lag > 1000:** Alert on-call team
- **Error rate > 1%:** Alert on-call team
- **Throughput drop > 50%:** Alert on-call team

## 11. Event Standards Checklist

- [ ] Event name follows naming convention
- [ ] All required fields present
- [ ] Schema version included
- [ ] Tenant context included
- [ ] Event is idempotent
- [ ] Consumer handles duplicates
- [ ] Error handling with DLQ configured
- [ ] Schema registered in Schema Registry
- [ ] Topic follows naming convention
- [ ] Consumer group follows naming convention
- [ ] Monitoring and alerts configured
