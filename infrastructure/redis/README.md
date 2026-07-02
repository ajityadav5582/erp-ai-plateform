# ERP AI Platform - Redis Infrastructure

Enterprise-grade Redis setup for caching, session management, and distributed locking.

## Architecture

```
infrastructure/redis/
├── redis.conf               # Redis server configuration
├── README.md                # This documentation
└── backups/
    └── backup.sh            # Backup automation script
```

## Use Cases

Redis serves multiple roles in the ERP AI Platform. Below are common patterns with practical examples.

### Caching

Cache frequently accessed database query results to reduce load and improve response times.

```java
// Spring Data Redis example
@Autowired
private RedisTemplate<String, Object> redisTemplate;

public Optional<Invoice> getInvoice(Long id) {
    String key = "invoice:" + id;
    ValueOperations<String, Object> ops = redisTemplate.opsForValue();

    // Try cache first
    Invoice cached = (Invoice) ops.get(key);
    if (cached != null) {
        return Optional.of(cached);
    }

    // Fallback to database
    Optional<Invoice> invoice = invoiceRepository.findById(id);
    invoice.ifPresent(inv -> ops.set(key, inv, 10, TimeUnit.MINUTES));

    return invoice;
}
```

```python
# Python with redis-py
import redis
import json

r = redis.Redis(host='localhost', port=6379, password='erpai_dev_password')

def get_invoice(invoice_id):
    key = f"invoice:{invoice_id}"
    cached = r.get(key)

    if cached:
        return json.loads(cached)

    # Fetch from database
    invoice = db.query("SELECT * FROM invoices WHERE id = %s", invoice_id)

    if invoice:
        r.setex(key, 600, json.dumps(invoice))  # 10 minutes

    return invoice
```

### Session Management

Store user session data with automatic expiration for stateless authentication.

```java
// Spring Session with Redis
@Bean
public RedisIndexedSessionRepository sessionRepository(RedisConnectionFactory factory) {
    RedisIndexedSessionRepository repo = new RedisIndexedSessionRepository(factory);
    repo.setDefaultMaxInactiveInterval(1800); // 30 minutes
    return repo;
}
```

```bash
# Inspect active sessions
redis-cli -a $REDIS_PASSWORD -n 1 KEYS "sessions:*"
redis-cli -a $REDIS_PASSWORD -n 1 TTL "sessions:user:123"
```

### Rate Limiting

Implement sliding window rate limiting for API endpoints.

```java
// Sliding window rate limiter
public boolean isAllowed(String userId, String action, int limit, int windowSeconds) {
    String key = String.format("ratelimit:%s:%s:%d", userId, action, System.currentTimeMillis() / 1000 / windowSeconds);
    Long count = redisTemplate.opsForValue().increment(key);

    if (count == 1) {
        redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
    }

    return count <= limit;
}
```

```python
# Fixed window rate limiter
import time

def check_rate_limit(user_id, action, limit=100, window=60):
    key = f"ratelimit:{user_id}:{action}:{int(time.time()) // window}"
    current = r.incr(key)

    if current == 1:
        r.expire(key, window)

    return current <= limit
```

### Distributed Locking

Prevent race conditions in distributed systems using Redis locks.

```java
// Redisson distributed lock
@Autowired
private RedissonClient redisson;

public void processPayment(Long orderId) {
    RLock lock = redisson.getLock("payment:order:" + orderId);

    try {
        // Wait up to 10 seconds, lock for 30 seconds
        boolean acquired = lock.tryLock(10, 30, TimeUnit.SECONDS);

        if (!acquired) {
            throw new ConcurrentProcessingException("Could not acquire lock");
        }

        // Critical section
        processOrderPayment(orderId);

    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new PaymentProcessingException(e);
    } finally {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

```python
# Redis lock with context manager
import redis
from redis.lock import Lock

r = redis.Redis(host='localhost', port=6379, password='erpai_dev_password')

def process_payment(order_id):
    lock = Lock(r, f"payment:order:{order_id}", timeout=30, blocking_timeout=10)

    with lock:
        # Critical section - only one process can execute this
        db.execute("UPDATE orders SET status = 'processing' WHERE id = %s", order_id)
        process_order_payment(order_id)
```

### Pub/Sub Messaging

Real-time event notifications between microservices.

```java
// Redis Pub/Sub listener
@Component
public class InventoryEventListener {

    @RedisListener("inventory:events")
    public void handleInventoryEvent(InventoryEvent event) {
        switch (event.getType()) {
            case STOCK_LOW:
                notifyProcurement(event.getProductId());
                break;
            case STOCK_UPDATED:
                updateProductCache(event.getProductId());
                break;
        }
    }
}

// Publishing events
public void publishStockUpdate(Long productId, int newQuantity) {
    InventoryEvent event = new InventoryEvent(
        "STOCK_UPDATED",
        productId,
        newQuantity,
        Instant.now()
    );
    redisTemplate.convertAndSend("inventory:events", event);
}
```

```python
# Redis Pub/Sub subscriber
import redis

r = redis.Redis(host='localhost', port=6379, password='erpai_dev_password')
pubsub = r.pubsub()
pubsub.subscribe("inventory:events")

for message in pubsub.listen():
    if message["type"] == "message":
        event = json.loads(message["data"])
        if event["type"] == "STOCK_LOW":
            send_procurement_alert(event["product_id"])
```

### Leaderboard / Ranking

Sorted sets for real-time leaderboards and ranking systems.

```java
// Sales leaderboard
public void recordSale(Long userId, BigDecimal amount) {
    String key = "leaderboard:sales:" + YearMonth.now();
    redisTemplate.opsForZSet().add(key, userId, amount.doubleValue());
}

public List<Long> getTopSalesPeople(int limit) {
    String key = "leaderboard:sales:" + YearMonth.now();
    Set<ZSetOperations.TypedTuple<Long>> tuples =
        redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, limit - 1);

    return tuples.stream()
        .map(ZSetOperations.TypedTuple::getValue)
        .collect(Collectors.toList());
}
```

```bash
# Get top 10 salespeople
redis-cli -a $REDIS_PASSWORD ZREVRANGE leaderboard:sales:2026-07 0 9 WITHSCORES
```

### Job Queue

Simple task queue for background job processing.

```java
// Redis list as a queue
public void enqueueJob(String jobType, String payload) {
    String job = String.format("%s:%s:%d", jobType, payload, System.currentTimeMillis());
    redisTemplate.opsForList().rightPush("queue:jobs", job);
}

public String dequeueJob() {
    return redisTemplate.opsForList().leftPop("queue:jobs", 5, TimeUnit.SECONDS);
}
```

```python
# Reliable queue with acknowledgment
def enqueue_job(queue_name, job_data):
    job = json.dumps({
        "id": str(uuid.uuid4()),
        "data": job_data,
        "created_at": time.time()
    })
    r.lpush(f"queue:{queue_name}", job)

def process_jobs(queue_name):
    while True:
        # Block for up to 5 seconds waiting for a job
        result = r.brpop(f"queue:{queue_name}", timeout=5)

        if result:
            _, job_data = result
            job = json.loads(job_data)
            try:
                process_job(job)
            except Exception as e:
                # Re-queue on failure
                r.lpush(f"queue:{queue_name}:failed", job_data)
                log.error(f"Job failed: {job['id']}", exc_info=e)
```

### Counters and Analytics

Atomic counters for real-time analytics and metrics.

```java
// Page view counter
public void recordPageView(String page) {
    String key = String.format("analytics:pageviews:%s:%s",
        page, LocalDate.now());
    redisTemplate.opsForValue().increment(key);
}

public long getPageViews(String page, LocalDate date) {
    String key = String.format("analytics:pageviews:%s:%s", page, date);
    Long count = redisTemplate.opsForValue().get(key);
    return count != null ? count : 0;
}
```

```bash
# Increment and get counter
redis-cli -a $REDIS_PASSWORD INCR analytics:pageviews:/invoices:2026-07-02
redis-cli -a $REDIS_PASSWORD GET analytics:pageviews:/invoices:2026-07-02
```

## Configuration

### Redis Settings

| Setting | Value | Purpose |
|---------|-------|---------|
| `bind` | 0.0.0.0 | Listen on all interfaces |
| `port` | 6379 | Default Redis port |
| `databases` | 16 | Number of logical databases |
| `appendonly` | yes | Enable AOF persistence |
| `appendfsync` | everysec | Balance between performance and durability |
| `maxmemory` | 256mb | Memory limit (configurable) |
| `maxmemory-policy` | allkeys-lru | Evict least recently used keys |
| `tcp-backlog` | 511 | TCP connection backlog |
| `tcp-keepalive` | 300 | TCP keepalive interval (seconds) |
| `timeout` | 0 | Close idle connections (0 = disabled) |
| `maxclients` | 10000 | Maximum client connections |

### Snapshotting (RDB)

| Setting | Value | Purpose |
|---------|-------|---------|
| `save 60 1` | - | Save if 1 key changed in 60s |
| `save 300 10` | - | Save if 10 keys changed in 300s |
| `save 60 10000` | - | Save if 10000 keys changed in 60s |
| `stop-writes-on-bgsave-error` | yes | Stop writes on RDB error |
| `rdbcompression` | yes | Compress RDB files |
| `rdbchecksum` | yes | Verify RDB file integrity |

### Persistence (AOF)

| Setting | Value | Purpose |
|---------|-------|---------|
| `appendonly` | yes | Enable append-only file |
| `appendfsync` | everysec | Sync every second (good balance) |
| `auto-aof-rewrite-percentage` | 100 | Rewrite when AOF doubles in size |
| `auto-aof-rewrite-min-size` | 64mb | Minimum size for AOF rewrite |
| `aof-load-truncated` | yes | Load truncated AOF on startup |
| `no-appendfsync-on-rewrite` | yes | Don't fsync during AOF rewrite |

### Memory Management

| Setting | Value | Purpose |
|---------|-------|---------|
| `maxmemory` | 256mb | Maximum memory usage |
| `maxmemory-policy` | allkeys-lru | Eviction policy when memory limit reached |

### Security

| Setting | Value | Purpose |
|---------|-------|---------|
| `protected-mode` | no | Disabled (password auth required) |
| `requirepass` | ${REDIS_PASSWORD} | Password authentication (set via command line) |

## Docker Compose

### Start Redis

```bash
# Start Redis only
docker compose -f compose.base.yml -f compose.infrastructure.yml up redis

# Start all infrastructure services
docker compose -f compose.base.yml -f compose.infrastructure.yml up
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `REDIS_PASSWORD` | `erpai_dev_password` | Redis authentication password |

### Docker Configuration

```yaml
services:
  redis:
    image: redis:7-alpine
    container_name: erpai-redis
    restart: unless-stopped
    user: redis
    command: >
      redis-server
      /usr/local/etc/redis/redis.conf
      --requirepass ${REDIS_PASSWORD:-erpai_dev_password}
      --maxmemory 256mb
      --maxmemory-policy allkeys-lru
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
      - ./infrastructure/redis/redis.conf:/usr/local/etc/redis/redis.conf:ro
    networks:
      - erpai-network
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD:-erpai_dev_password}", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: '0.5'
        reservations:
          memory: 256M
          cpus: '0.25'
    cap_drop:
      - ALL
    cap_add:
      - CHOWN
      - SETGID
      - SETUID
      - DAC_OVERRIDE
    read_only: true
    tmpfs:
      - /tmp
      - /var/run
```

## Usage

### Connect to Redis

```bash
# Using redis-cli
docker compose exec redis redis-cli -a $REDIS_PASSWORD

# Using host connection
redis-cli -h localhost -p 6379 -a $REDIS_PASSWORD
```

### Test Connection

```bash
# Ping Redis
docker compose exec redis redis-cli -a $REDIS_PASSWORD ping
# Response: PONG

# Check memory usage
docker compose exec redis redis-cli -a $REDIS_PASSWORD info memory
```

### Database Selection

Redis provides 16 logical databases (0-15). Use them for:

| Database | Purpose |
|----------|---------|
| 0 | Default (caching) |
| 1 | Sessions |
| 2 | Rate limiting |
| 3 | Distributed locks |
| 4-15 | Application-specific |

```bash
# Select database
docker compose exec redis redis-cli -a $REDIS_PASSWORD -n 1 ping
```

## Health Checks

### Docker Health Check

The Redis container includes a health check that verifies:

1. Redis is responding to PING commands
2. Authentication is working correctly

```bash
# Check container health
docker compose ps redis

# View health check logs
docker inspect --format='{{json .State.Health}}' erpai-redis
```

### Manual Health Check

```bash
# Basic connectivity
redis-cli -h localhost -p 6379 ping

# Authenticated check
redis-cli -h localhost -p 6379 -a $REDIS_PASSWORD ping

# Check if Redis is ready
docker compose exec redis redis-cli -a $REDIS_PASSWORD info persistence

# Check memory usage
docker compose exec redis redis-cli -a $REDIS_PASSWORD info memory

# Check connected clients
docker compose exec redis redis-cli -a $REDIS_PASSWORD info clients

# Check keyspace statistics
docker compose exec redis redis-cli -a $REDIS_PASSWORD info keyspace
```

## Persistence

### Volume

Redis data is persisted to the `redis_data` volume:

```bash
# Location: Docker managed volume
# Path inside container: /data
# Files:
#   - dump.rdb (snapshot)
#   - appendonly.aof (append-only file)
```

### Backup

```bash
# Create a backup using the backup script
bash infrastructure/redis/backups/backup.sh

# Manual backup - trigger background save
docker compose exec redis redis-cli -a $REDIS_PASSWORD BGSAVE

# Copy the dump file
docker compose cp erpai-redis:/data/dump.rdb ./backups/

# For AOF backup
docker compose cp erpai-redis:/data/appendonly.aof ./backups/
```

### Restore

```bash
# Stop Redis
docker compose stop redis

# Copy backup to volume
docker compose cp ./backups/dump.rdb erpai-redis:/data/

# Start Redis
docker compose start redis
```

## Security

### Password Authentication

Redis requires password authentication:

```bash
# Set password (via environment variable)
REDIS_PASSWORD=your_secure_password

# Connect with password
redis-cli -a $REDIS_PASSWORD
```

### Container Security

The Redis container is hardened with:

- **Non-root user**: Runs as `redis` user (UID 999)
- **Read-only filesystem**: Container filesystem is read-only
- **Dropped capabilities**: All Linux capabilities dropped except essential ones
- **Tmpfs mounts**: `/tmp` and `/var/run` mounted as tmpfs for writable paths

### Production Recommendations

1. Use a strong, unique password (minimum 32 characters)
2. Consider using Redis ACL for fine-grained permissions
3. Enable TLS in production (requires Redis 6+)
4. Use network isolation (Docker networks)
5. Consider Redis Sentinel for high availability
6. Enable Redis Cluster for horizontal scaling
7. Regularly rotate passwords
8. Monitor for suspicious access patterns

### ACL Configuration (Production)

```bash
# Create ACL file
cat > /tmp/redis.acl << EOF
user default on nopass ~* +@all
user app on >app_password ~caching:* +@read +@write -@admin
user app on >app_password ~sessions:* +@read +@write -@admin
user monitoring on >monitor_password ~* +@read -@write -@admin
EOF

# Load ACL
docker compose exec redis redis-cli -a $REDIS_PASSWORD ACL LOAD /tmp/redis.acl
```

## Monitoring

### Key Metrics

```bash
# Memory usage
redis-cli -a $REDIS_PASSWORD info memory

# Connected clients
redis-cli -a $REDIS_PASSWORD info clients

# Key statistics
redis-cli -a $REDIS_PASSWORD info keyspace

# Persistence status
redis-cli -a $REDIS_PASSWORD info persistence

# Command statistics
redis-cli -a $REDIS_PASSWORD info stats

# Replication status
redis-cli -a $REDIS_PASSWORD info replication
```

### Slow Log

```bash
# Get slow log entries
redis-cli -a $REDIS_PASSWORD slowlog get 10

# Reset slow log
redis-cli -a $REDIS_PASSWORD slowlog reset

# Set slow log threshold (microseconds)
redis-cli -a $REDIS_PASSWORD CONFIG SET slowlog-log-slower-than 10000
```

### Redis Exporter

For Prometheus integration, add redis-exporter:

```yaml
services:
  redis-exporter:
    image: oliver006/redis_exporter:latest
    container_name: erpai-redis-exporter
    ports:
      - "9121:9121"
    environment:
      REDIS_ADDR: redis://erpai-redis:6379
      REDIS_PASSWORD: ${REDIS_PASSWORD:-erpai_dev_password}
    networks:
      - erpai-network
```

## Troubleshooting

### Connection Issues

```bash
# Check if Redis is running
docker compose ps redis

# Check Redis logs
docker compose logs redis

# Test port connectivity
nc -zv localhost 6379

# Check if password is set
docker compose exec redis redis-cli ping
# Should return: (error) NOAUTH Authentication required.
```

### Memory Issues

```bash
# Check memory usage
docker compose exec redis redis-cli -a $REDIS_PASSWORD info memory

# Check eviction stats
docker compose exec redis redis-cli -a $REDIS_PASSWORD info stats | grep evicted

# Check memory fragmentation
docker compose exec redis redis-cli -a $REDIS_PASSWORD info memory | grep mem_fragmentation_ratio

# Clear all data (development only)
docker compose exec redis redis-cli -a $REDIS_PASSWORD FLUSHALL
```

### Persistence Issues

```bash
# Check persistence status
docker compose exec redis redis-cli -a $REDIS_PASSWORD info persistence

# Force a background save
docker compose exec redis redis-cli -a $REDIS_PASSWORD BGSAVE

# Check if AOF is enabled
docker compose exec redis redis-cli -a $REDIS_PASSWORD CONFIG GET appendonly

# Check last save time
docker compose exec redis redis-cli -a $REDIS_PASSWORD info persistence | grep rdb_last_save_time
```

### Performance Issues

```bash
# Check slow log
redis-cli -a $REDIS_PASSWORD slowlog get 10

# Check command stats
redis-cli -a $REDIS_PASSWORD info stats

# Monitor commands in real-time
redis-cli -a $REDIS_PASSWORD monitor

# Check latency
redis-cli -a $REDIS_PASSWORD --latency
redis-cli -a $REDIS_PASSWORD --latency-history
```

## Backup & Recovery

### Automated Backups

The backup script (`infrastructure/redis/backups/backup.sh`) provides:

- RDB snapshot backup
- AOF backup
- Backup verification
- Automatic cleanup of old backups

### Manual Backup

```bash
# Trigger background save
docker compose exec redis redis-cli -a $REDIS_PASSWORD BGSAVE

# Wait for completion
docker compose exec redis redis-cli -a $REDIS_PASSWORD info persistence | grep rdb_bgsave_in_progress

# Copy files from volume
docker compose cp erpai-redis:/data/dump.rdb ./backups/
docker compose cp erpai-redis:/data/appendonly.aof ./backups/
```

### Restore from Backup

```bash
# Stop Redis
docker compose stop redis

# Remove existing data
docker compose rm -f redis
docker volume rm erpai-platform_redis_data

# Restore backup
docker compose cp ./backups/dump.rdb erpai-redis:/data/
docker compose cp ./backups/appendonly.aof erpai-redis:/data/

# Start Redis
docker compose up -d redis

# Verify restoration
docker compose exec redis redis-cli -a $REDIS_PASSWORD ping
```

## References

- [Redis Documentation](https://redis.io/documentation)
- [Redis Persistence](https://redis.io/topics/persistence)
- [Redis Security](https://redis.io/topics/security)
- [Redis Administration](https://redis.io/topics/admin)
- [Docker Standards](../docs/standards/16-docker-standards.md)
- [Database Standards](../docs/standards/04-database-standards.md)
