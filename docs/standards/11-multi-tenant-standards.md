# Multi-Tenant Standards

## 1. Purpose

These standards ensure secure, scalable, and maintainable multi-tenancy across the ERP SaaS platform. Multi-tenancy is a core architectural requirement that must be consistently implemented.

## 2. Multi-Tenancy Principles

- **Isolation first:** Tenant data must be completely isolated
- **Scalability:** Support thousands of tenants efficiently
- **Performance:** No cross-tenant performance degradation
- **Security:** Prevent data leakage between tenants
- **Maintainability:** Easy to add/remove tenants
- **Cost efficiency:** Shared resources where safe

## 3. Isolation Strategies

### 3.1 Strategy Comparison

| Strategy | Isolation | Scalability | Complexity | Cost | Use Case |
|----------|-----------|-------------|------------|------|----------|
| Separate Database | Highest | Low | Low | High | Enterprise, compliance |
| Separate Schema | High | Medium | Medium | Medium | Mid-market |
| Shared Schema (Discriminator) | Medium | High | High | Low | SMB, high scale |

### 3.2 Recommended: Schema-per-Tenant

For the ERP platform, we use **schema-per-tenant** for enterprise customers and **shared schema with discriminator** for SMB customers.

## 4. Schema-per-Tenant Implementation

### 4.1 Database Setup

```sql
-- Create tenant schema
CREATE SCHEMA tenant_12345;
CREATE SCHEMA tenant_67890;

-- Grant permissions
GRANT USAGE ON SCHEMA tenant_12345 TO app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA tenant_12345 TO app_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA tenant_12345 TO app_user;
```

### 4.2 Dynamic Schema Resolution

```java
@Component
public class MultiTenantConnectionProvider implements AbstractDataSourceBasedMultiTenantConnectionProviderImpl {
    
    private final DataSourceResolver dataSourceResolver;
    private final Map<Object, Object> dataSources = new ConcurrentHashMap<>();
    
    @Override
    protected DataSource resolveDataSource(Object tenantId) {
        return dataSources.computeIfAbsent(tenantId, id -> 
            dataSourceResolver.resolveDataSourceForTenant((Long) id));
    }
    
    @Override
    public Collection<Object> getResolvedTenantIds() {
        return dataSources.keySet();
    }
}
```

### 4.3 Tenant Context

```java
@Component
public class TenantContextHolder {
    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();
    
    public void setTenantId(Long tenantId) {
        CURRENT_TENANT.set(tenantId);
    }
    
    public Long getTenantId() {
        Long tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant context set");
        }
        return tenantId;
    }
    
    public void clear() {
        CURRENT_TENANT.remove();
    }
}
```

### 4.4 Tenant Interceptor

```java
@Component
public class TenantInterceptor implements HandlerInterceptor {
    
    private final TenantContextHolder tenantContextHolder;
    private final TenantService tenantService;
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {
        String tenantId = request.getHeader("X-Tenant-ID");
        
        if (tenantId == null || tenantId.isBlank()) {
            throw new MissingTenantException("X-Tenant-ID header is required");
        }
        
        Long tenantIdLong = Long.parseLong(tenantId);
        
        // Validate tenant exists and is active
        Tenant tenant = tenantService.findById(tenantIdLong)
            .orElseThrow(() -> new TenantNotFoundException(tenantIdLong));
        
        if (!tenant.isActive()) {
            throw new TenantInactiveException(tenantIdLong);
        }
        
        tenantContextHolder.setTenantId(tenantIdLong);
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, 
                               HttpServletResponse response, 
                               Object handler, 
                               Exception ex) {
        tenantContextHolder.clear();
    }
}
```

## 5. Shared Schema Implementation

### 5.1 Tenant Discriminator

```java
@Entity
@Table(name = "invoices")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    
    @Column(name = "invoice_number", nullable = false)
    private String invoiceNumber;
    
    // ... other fields
    
    @PrePersist
    void prePersist() {
        if (tenantId == null) {
            tenantId = TenantContextHolder.getTenantId();
        }
    }
}
```

### 5.2 Global Filter

```java
@Component
public class TenantFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain)
            throws IOException, ServletException {
        try {
            String tenantId = ((HttpServletRequest) request).getHeader("X-Tenant-ID");
            if (tenantId != null) {
                TenantContextHolder.setTenantId(Long.parseLong(tenantId));
            }
            chain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
```

### 5.3 Repository with Tenant Filter

```java
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    
    // Automatically filtered by tenant
    @Override
    @TenantFilter  // Custom annotation or AOP
    List<Invoice> findAll();
    
    @TenantFilter
    Optional<Invoice> findById(Long id);
    
    // Explicit tenant filter for custom queries
    @Query("SELECT i FROM Invoice i WHERE i.tenantId = :tenantId AND i.id = :id")
    Optional<Invoice> findByIdAndTenant(@Param("tenantId") Long tenantId, 
                                        @Param("id") Long id);
}
```

### 5.4 AOP Tenant Filter

```java
@Aspect
@Component
public class TenantFilterAspect {
    
    @Around("@annotation(com.erp.tenancy.TenantFilter) || " +
            "@within(com.erp.tenancy.TenantFilter)")
    public Object filterByTenant(ProceedingJoinPoint joinPoint) throws Throwable {
        Long tenantId = TenantContextHolder.getTenantId();
        
        // Add tenant filter to queries
        // Implementation depends on JPA provider
        
        return joinPoint.proceed();
    }
}
```

## 6. Tenant Provisioning

### 6.1 Provisioning Flow

```java
@Service
@RequiredArgsConstructor
public class TenantProvisioningService {
    private final TenantRepository tenantRepository;
    private final DatabaseProvisioningService databaseProvisioning;
    private final SchemaProvisioningService schemaProvisioning;
    private final InitialDataLoader initialDataLoader;
    
    @Transactional
    public Tenant provisionTenant(TenantProvisioningRequest request) {
        // 1. Create tenant record
        Tenant tenant = Tenant.builder()
            .name(request.name())
            .subdomain(request.subdomain())
            .plan(request.plan())
            .status(TenantStatus.ACTIVE)
            .build();
        tenant = tenantRepository.save(tenant);
        
        // 2. Provision database resources
        if (request.isolationStrategy() == IsolationStrategy.SCHEMA_PER_TENANT) {
            schemaProvisioning.createSchema(tenant.getId());
        } else if (request.isolationStrategy() == IsolationStrategy.DATABASE_PER_TENANT) {
            databaseProvisioning.createDatabase(tenant.getId());
        }
        
        // 3. Load initial data
        initialDataLoader.loadInitialData(tenant.getId(), request.initialData());
        
        // 4. Configure tenant settings
        tenantConfigurationService.configureDefaults(tenant.getId());
        
        return tenant;
    }
}
```

### 6.2 Schema Provisioning

```java
@Service
@RequiredArgsConstructor
public class SchemaProvisioningService {
    private final JdbcTemplate jdbcTemplate;
    
    @Transactional
    public void createSchema(Long tenantId) {
        String schemaName = "tenant_" + tenantId;
        
        // Create schema
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
        
        // Run migrations
        flyway.configure(new FlywayConfiguration())
            .schemas(schemaName)
            .load()
            .migrate();
        
        // Grant permissions
        jdbcTemplate.execute(String.format(
            "GRANT USAGE ON SCHEMA %s TO app_user", schemaName));
    }
    
    @Transactional
    public void dropSchema(Long tenantId) {
        String schemaName = "tenant_" + tenantId;
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + schemaName + " CASCADE");
    }
}
```

## 7. Tenant Lifecycle

### 7.1 States

```java
public enum TenantStatus {
    PENDING,      // Provisioning in progress
    ACTIVE,       // Normal operation
    SUSPENDED,    // Temporarily disabled (non-payment)
    DEACTIVATED,  // Permanently disabled
    ARCHIVED      // Data archived, tenant removed
}
```

### 7.2 Lifecycle Operations

```java
@Service
public class TenantLifecycleService {
    
    public void suspendTenant(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));
        
        tenant.setStatus(TenantStatus.SUSPENDED);
        tenant.setSuspendedAt(Instant.now());
        tenantRepository.save(tenant);
        
        // Disable access
        tenantAccessService.disableAccess(tenantId);
        
        // Notify tenant
        notificationService.sendTenantSuspendedNotification(tenant);
    }
    
    public void reactivateTenant(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));
        
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setSuspendedAt(null);
        tenantRepository.save(tenant);
        
        // Enable access
        tenantAccessService.enableAccess(tenantId);
    }
    
    public void deactivateTenant(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));
        
        tenant.setStatus(TenantStatus.DEACTIVATED);
        tenant.setDeactivatedAt(Instant.now());
        tenantRepository.save(tenant);
        
        // Disable access
        tenantAccessService.disableAccess(tenantId);
        
        // Archive data
        dataArchivalService.archiveTenantData(tenantId);
    }
}
```

## 8. Cross-Tenant Operations

### 8.1 Prohibited Operations

```java
// NEVER: Cross-tenant queries without explicit tenant filter
@Query("SELECT i FROM Invoice i WHERE i.total > :amount")
List<Invoice> findLargeInvoices(@Param("amount") BigDecimal amount);

// NEVER: Join across tenant schemas
@Query("SELECT t1.*, t2.* FROM tenant_123.invoices t1 " +
       "JOIN tenant_456.invoices t2 ON t1.customer_id = t2.customer_id")
```

### 8.2 Allowed Cross-Tenant Operations

```java
// Platform admin operations with explicit tenant context
@Service
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlatformAdminService {
    
    @TenantContext(tenantId = "#{tenantId}")  // Explicit tenant context
    public TenantReport generateReport(Long tenantId, ReportRequest request) {
        // Operations run in tenant context
        return reportService.generate(tenantId, request);
    }
}
```

## 9. Tenant Configuration

### 9.1 Tenant Settings

```java
@Entity
@Table(name = "tenant_settings")
public class TenantSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    
    @Column(name = "setting_key", nullable = false)
    private String key;
    
    @Column(name = "setting_value", columnDefinition = "JSONB")
    private String value;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
}
```

### 9.2 Feature Flags per Tenant

```java
@Service
public class FeatureFlagService {
    
    public boolean isFeatureEnabled(Long tenantId, String feature) {
        TenantFeatureFlag flag = tenantFeatureFlagRepository
            .findByTenantIdAndFeature(tenantId, feature)
            .orElse(TenantFeatureFlag.disabled(feature));
        
        return flag.isEnabled();
    }
    
    public void enableFeature(Long tenantId, String feature) {
        TenantFeatureFlag flag = TenantFeatureFlag.builder()
            .tenantId(tenantId)
            .feature(feature)
            .enabled(true)
            .build();
        tenantFeatureFlagRepository.save(flag);
    }
}
```

## 10. Tenant Metrics

### 10.1 Per-Tenant Metrics

- **Request count:** Per tenant
- **Error rate:** Per tenant
- **Response time:** Per tenant (p50, p95, p99)
- **Storage usage:** Per tenant
- **Active users:** Per tenant

### 10.2 Monitoring

```java
@Component
public class TenantMetricsAspect {
    
    @Around("@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PostMapping)")
    public Object collectMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        Long tenantId = TenantContextHolder.getTenantId();
        long start = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            
            // Record metrics
            Metrics.recordRequest(tenantId, joinPoint.getSignature().getDeclaringTypeName());
            Metrics.recordLatency(tenantId, duration);
            
            return result;
        } catch (Exception e) {
            Metrics.recordError(tenantId);
            throw e;
        }
    }
}
```

## 11. Tenant Data Export/Import

### 11.1 Export

```java
@Service
public class TenantDataExportService {
    
    public TenantExport exportTenantData(Long tenantId) {
        return TenantExport.builder()
            .tenantId(tenantId)
            .exportedAt(Instant.now())
            .invoices(invoiceRepository.findAllByTenantId(tenantId))
            .customers(customerRepository.findAllByTenantId(tenantId))
            .products(productRepository.findAllByTenantId(tenantId))
            .build();
    }
}
```

### 11.2 Import

```java
@Service
public class TenantDataImportService {
    
    @Transactional
    public void importTenantData(Long targetTenantId, TenantExport export) {
        // Validate export belongs to source tenant
        // Map IDs to new tenant
        // Import with new tenant context
    }
}
```

## 12. Multi-Tenant Checklist

- [ ] Tenant isolation strategy defined
- [ ] Tenant context propagated through all layers
- [ ] All queries filtered by tenant ID
- [ ] No cross-tenant data leakage possible
- [ ] Tenant provisioning automated
- [ ] Tenant lifecycle management implemented
- [ ] Per-tenant metrics and monitoring
- [ ] Tenant data export/import supported
- [ ] Feature flags per tenant
- [ ] Tenant-specific configuration supported
