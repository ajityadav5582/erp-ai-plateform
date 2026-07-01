# Package Structure Standards

## 1. Purpose

These standards ensure consistent, scalable, and maintainable package organization across the ERP SaaS platform. A well-defined package structure improves code navigation, separation of concerns, and team productivity.

## 2. Package Organization Principles

- **Domain-driven:** Organize by business domain, not technical layer
- **Bounded contexts:** Clear boundaries between domains
- **Layer separation:** application, domain, infrastructure, interfaces
- **Shared code:** Common utilities in shared libraries
- **No circular dependencies:** Dependencies flow inward

## 3. Root Package Structure

```
com.erp.platform/
├── ai/                          # AI/ML modules
│   ├── agents/
│   ├── orchestration/
│   └── pipeline/
├── business/                    # Business domain modules
│   ├── finance/
│   ├── hr/
│   ├── inventory/
│   ├── manufacturing/
│   ├── procurement/
│   └── sales/
├── integration/                 # Integration modules
│   ├── events/
│   ├── external/
│   ├── gateway/
│   └── messaging/
├── platform/                    # Platform core
│   ├── core/
│   └── shared/
├── libraries/                   # Shared libraries
│   ├── common/
│   ├── data/
│   ├── observability/
│   └── testing/
└── infrastructure/              # Infrastructure code
    ├── docker/
    ├── k8s/
    └── terraform/
```

## 4. Bounded Context Package Structure

Each bounded context follows this structure:

```
com.erp.platform.business.finance/
├── application/                 # Application layer
│   ├── commands/                # Command handlers
│   ├── queries/                 # Query handlers
│   ├── dto/                     # Request/Response DTOs
│   ├── mappers/                 # DTO to domain mappers
│   └── services/                # Application services
├── domain/                      # Domain layer
│   ├── model/                   # Domain models/entities
│   ├── valueobject/             # Value objects
│   ├── repository/              # Repository interfaces
│   ├── service/                 # Domain services
│   ├── event/                   # Domain events
│   └── exception/               # Domain exceptions
├── infrastructure/              # Infrastructure layer
│   ├── persistence/             # JPA repositories, entities
│   ├── messaging/               # Event publishers/consumers
│   ├── external/                # External API clients
│   └── config/                  # Configuration classes
└── interfaces/                  # Interface layer
    ├── rest/                    # REST controllers
    ├── grpc/                    # gRPC services (if used)
    └── scheduler/               # Scheduled jobs
```

## 5. Layer Responsibilities

### 5.1 Application Layer

```java
// Commands - Write operations
package com.erp.platform.business.finance.application.commands;

public record CreateInvoiceCommand(
    Long tenantId,
    Long customerId,
    BigDecimal total,
    String currency,
    LocalDate dueDate,
    List<CreateInvoiceLineItemCommand> lineItems
) {}

@Component
public class CreateInvoiceCommandHandler {
    private final InvoiceService invoiceService;
    
    public InvoiceResponse handle(CreateInvoiceCommand command) {
        return invoiceService.create(command);
    }
}

// Queries - Read operations
package com.erp.platform.business.finance.application.queries;

public record GetInvoiceQuery(Long tenantId, Long invoiceId) {}

@Component
public class GetInvoiceQueryHandler {
    private final InvoiceQueryService invoiceQueryService;
    
    public InvoiceResponse handle(GetInvoiceQuery query) {
        return invoiceQueryService.findById(query.tenantId(), query.invoiceId());
    }
}

// DTOs
package com.erp.platform.business.finance.application.dto;

@Data
@Builder
public class CreateInvoiceRequest {
    private Long customerId;
    private BigDecimal total;
    private String currency;
    private LocalDate dueDate;
    private List<CreateInvoiceLineItemRequest> lineItems;
}

@Data
@Builder
public class InvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private BigDecimal total;
    private String currency;
    private InvoiceStatus status;
    private LocalDate dueDate;
}
```

### 5.2 Domain Layer

```java
// Domain model
package com.erp.platform.business.finance.domain.model;

@Entity
@Table(name = "invoices")
public class Invoice {
    @Id
    private Long id;
    
    @Column(nullable = false)
    private Long tenantId;
    
    @Column(nullable = false, unique = true)
    private String invoiceNumber;
    
    @Column(nullable = false)
    private BigDecimal total;
    
    @Column(nullable = false)
    private InvoiceStatus status;
    
    // Business methods
    public void markAsPaid() {
        if (this.status != InvoiceStatus.PENDING) {
            throw new IllegalStateException("Only pending invoices can be marked as paid");
        }
        this.status = InvoiceStatus.PAID;
        this.paidAt = Instant.now();
    }
    
    public void cancel() {
        if (this.status == InvoiceStatus.PAID) {
            throw new IllegalStateException("Paid invoices cannot be cancelled");
        }
        this.status = InvoiceStatus.CANCELLED;
        this.cancelledAt = Instant.now();
    }
}

// Value object
package com.erp.platform.business.finance.domain.valueobject;

@Embeddable
public record Money(
    @Column(nullable = false)
    BigDecimal amount,
    
    @Column(nullable = false, length = 3)
    Currency currency
) {
    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }
    
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    private void validateSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                String.format("Currency mismatch: %s vs %s", this.currency, other.currency));
        }
    }
}

// Repository interface
package com.erp.platform.business.finance.domain.repository;

public interface InvoiceRepository {
    Optional<Invoice> findById(Long tenantId, Long id);
    Invoice save(Invoice invoice);
    void delete(Invoice invoice);
    List<Invoice> findByTenantIdAndStatus(Long tenantId, InvoiceStatus status);
}

// Domain service
package com.erp.platform.business.finance.domain.service;

@Service
public class InvoiceDomainService {
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    
    public InvoiceNumber generateInvoiceNumber(Long tenantId) {
        String prefix = "INV-" + Year.now() + "-";
        String maxNumber = invoiceRepository.findMaxInvoiceNumber(tenantId, prefix);
        int nextNumber = maxNumber != null ? 
            Integer.parseInt(maxNumber.substring(prefix.length())) + 1 : 1;
        return new InvoiceNumber(prefix + String.format("%04d", nextNumber));
    }
}

// Domain event
package com.erp.platform.business.finance.domain.event;

public record InvoiceCreatedEvent(
    UUID eventId,
    Long tenantId,
    Long invoiceId,
    String invoiceNumber,
    BigDecimal total,
    Instant occurredAt
) implements DomainEvent {}

// Domain exception
package com.erp.platform.business.finance.domain.exception;

public class InvoiceNotFoundException extends DomainException {
    public InvoiceNotFoundException(Long tenantId, Long invoiceId) {
        super("INVOICE_NOT_FOUND", 
              String.format("Invoice %d not found for tenant %d", invoiceId, tenantId));
    }
}
```

### 5.3 Infrastructure Layer

```java
// JPA Repository
package com.erp.platform.business.finance.infrastructure.persistence;

@Repository
public interface JpaInvoiceRepository extends JpaRepository<InvoiceEntity, Long> {
    Optional<InvoiceEntity> findByTenantIdAndId(Long tenantId, Long id);
    List<InvoiceEntity> findByTenantIdAndStatus(Long tenantId, InvoiceStatus status);
}

// Repository implementation
package com.erp.platform.business.finance.infrastructure.persistence;

@Repository
@RequiredArgsConstructor
public class InvoiceRepositoryImpl implements InvoiceRepository {
    private final JpaInvoiceRepository jpaRepository;
    private final InvoiceEntityMapper mapper;
    
    @Override
    public Optional<Invoice> findById(Long tenantId, Long id) {
        return jpaRepository.findByTenantIdAndId(tenantId, id)
            .map(mapper::toDomain);
    }
    
    @Override
    public Invoice save(Invoice invoice) {
        InvoiceEntity entity = mapper.toEntity(invoice);
        InvoiceEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}

// Event publisher
package com.erp.platform.business.finance.infrastructure.messaging;

@Component
@RequiredArgsConstructor
public class InvoiceEventPublisher {
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    
    @EventListener
    public void handle(InvoiceCreatedEvent event) {
        kafkaTemplate.send("invoice.created", event);
    }
}
```

### 5.4 Interface Layer

```java
// REST Controller
package com.erp.platform.business.finance.interfaces.rest;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    private final CreateInvoiceCommandHandler createHandler;
    private final GetInvoiceQueryHandler getHandler;
    
    @PostMapping
    public ResponseEntity<InvoiceResponse> create(
            @Valid @RequestBody CreateInvoiceRequest request) {
        CreateInvoiceCommand command = CreateInvoiceCommand.builder()
            .tenantId(tenantContextHolder.getTenantId())
            .customerId(request.getCustomerId())
            .total(request.getTotal())
            .currency(request.getCurrency())
            .dueDate(request.getDueDate())
            .lineItems(request.getLineItems().stream()
                .map(item -> new CreateInvoiceLineItemCommand(
                    item.getDescription(), item.getQuantity(), item.getUnitPrice()))
                .toList())
            .build();
        
        InvoiceResponse response = createHandler.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> get(@PathVariable Long id) {
        GetInvoiceQuery query = new GetInvoiceQuery(tenantContextHolder.getTenantId(), id);
        return ResponseEntity.ok(getHandler.handle(query));
    }
}
```

## 6. Shared Libraries

### 6.1 Common Library

```
com.erp.platform.libraries.common/
├── events/                      # Event base classes
│   ├── DomainEvent.java
│   ├── IntegrationEvent.java
│   └── DomainEventEnvelope.java
├── exceptions/                  # Common exceptions
│   ├── ErpException.java
│   ├── BusinessException.java
│   └── ResourceNotFoundException.java
├── kernel/                      # Core abstractions
│   ├── AggregateRoot.java
│   ├── Entity.java
│   └── ValueObject.java
├── security/                    # Security utilities
│   ├── TenantContextHolder.java
│   └── SecurityUtils.java
├── tenancy/                     # Multi-tenancy utilities
│   ├── TenantAware.java
│   └── TenantFilter.java
└── utils/                       # General utilities
    ├── MoneyUtils.java
    └── DateUtils.java
```

### 6.2 Data Library

```
com.erp.platform.libraries.data/
├── flyway/                      # Flyway utilities
├── jpa/                         # JPA utilities
│   ├── AuditingEntityListener.java
│   └── SoftDeleteAspect.java
├── kafka/                       # Kafka utilities
├── minio/                       # MinIO/S3 utilities
└── redis/                       # Redis utilities
```

### 6.3 Observability Library

```
com.erp.platform.libraries.observability/
├── health/                      # Health checks
├── logging/                     # Logging utilities
├── metrics/                     # Metrics utilities
└── tracing/                     # Distributed tracing
```

## 7. Package Naming Conventions

| Package | Convention | Example |
|---------|-----------|---------|
| Domain | `{domain}.{aggregate}` | `finance.invoice` |
| Commands | `{domain}.application.commands` | `finance.application.commands` |
| Queries | `{domain}.application.queries` | `finance.application.queries` |
| DTOs | `{domain}.application.dto` | `finance.application.dto` |
| Repositories | `{domain}.domain.repository` | `finance.domain.repository` |
| Events | `{domain}.domain.event` | `finance.domain.event` |
| Controllers | `{domain}.interfaces.rest` | `finance.interfaces.rest` |

## 8. Dependency Rules

### 8.1 Allowed Dependencies

```
interfaces → application → domain
infrastructure → domain
```

### 8.2 Prohibited Dependencies

```
domain → application (NO)
domain → infrastructure (NO)
application → infrastructure (NO)
application → interfaces (NO)
```

### 8.3 Dependency Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    interfaces (REST, gRPC)                   │
│                     (depends on application)                 │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                    application (use cases)                   │
│              (depends on domain, uses DTOs)                  │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                    domain (business logic)                   │
│              (no external dependencies)                      │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│              infrastructure (implementations)                │
│              (depends on domain)                             │
└─────────────────────────────────────────────────────────────┘
```

## 9. Module Organization

### 9.1 Monorepo Structure

```
erp-ai-platform/
├── business-finance/            # Finance module
├── business-hr/                 # HR module
├── business-inventory/          # Inventory module
├── business-sales/              # Sales module
├── platform-core/               # Core platform
├── libraries-common/            # Common library
├── libraries-data/              # Data library
├── libraries-observability/     # Observability library
├── integration-events/          # Event infrastructure
├── integration-gateway/         # API Gateway
└── ai-orchestration/            # AI orchestration
```

### 9.2 Module Dependencies

```gradle
// business-finance/build.gradle
dependencies {
    implementation project(':platform-core')
    implementation project(':libraries-common')
    implementation project(':libraries-data')
    implementation project(':integration-events')
    
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
}
```

## 10. Package Structure Checklist

- [ ] Organized by bounded context
- [ ] Clear layer separation (application, domain, infrastructure, interfaces)
- [ ] No circular dependencies
- [ ] Domain layer has no external dependencies
- [ ] Shared code in libraries
- [ ] Consistent naming conventions
- [ ] Package names reflect business domains
- [ ] Infrastructure implementations in infrastructure layer
- [ ] Controllers thin, delegate to application layer
