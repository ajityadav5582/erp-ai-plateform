# Redis Infrastructure

Redis is used for caching, session management, and as a message broker for certain patterns in the ERP AI Platform.

## Overview

| Property | Value |
|----------|-------|
| **Image** | `redis:7-alpine` |
| **Port** | 6379 |
| **Container** | `erpai-redis` |
| **Purpose** | Caching, sessions, pub/sub |
| **Owner** | Platform Team |

## Use Cases

| Use Case | Description |
|----------|-------------|
| **Caching** | Cache frequently accessed data (products, users, settings) |
| **Sessions** | Store user session data for the gateway |
| **Rate Limiting** | Track API rate limits per user/IP |
| **Pub/Sub** | Real-time notifications and events |
| **Locks** | Distributed locks for critical sections |

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `REDIS_PASSWORD` | `erpai_dev_password` | Redis authentication password |

### Redis Settings

```conf
maxmemory 256mb
maxmemory-policy allkeys-lru
```

- **maxmemory**: Limits Redis to 256MB
- **maxmemory-policy**: `allkeys-lru` — evicts least recently used keys when memory limit is reached

### Security

- Runs as `redis` user (non-root)
- Requires password authentication
- Read-only filesystem with tmpfs for `/tmp` and `/var/run`
- Dropped all Linux capabilities except CHOWN, SETGID, SETUID, DAC_OVERRIDE

## Relationships

```
┌─────────────────────────────────────────────────────────────────┐
│                         Redis                                   │
│                                                                 │
│  ▲                                                               │
│  │                                                               │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  └──│  Gateway    │  │  Services   │  │  Exporters  │          │
│     │ (Sessions)  │  │  (Cache)    │  │ (Prometheus)│          │
│     └─────────────┘  └─────────────┘  └─────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

### Related Components

| Component | Relationship |
|-----------|--------------|
| **API Gateway** | Stores user sessions and rate limit counters |
| **Microservices** | Cache frequently accessed data |
| **Redis Exporter** | Scrapes metrics for Prometheus monitoring |

## Quick Start

```bash
# Start Redis only
docker compose -f compose.base.yml -f compose.infrastructure.yml up redis

# Test Redis connection
docker compose exec redis redis-cli -a $REDIS_PASSWORD ping

# Access Redis CLI
docker compose exec redis redis-cli -a $REDIS_PASSWORD

# Check Redis info
docker compose exec redis redis-cli -a $REDIS_PASSWORD INFO
```

## Data Structures

### Common Patterns

**String (Key-Value):**
```bash
# Set a value
SET user:123 "John Doe"

# Get a value
GET user:123

# Set with expiration (TTL)
EXPIRE user:123 3600
```

**Hash (Object):**
```bash
# Store a user object
HSET user:123 name "John Doe" email "john@example.com" role "admin"

# Get a field
HGET user:123 email

# Get all fields
HGETALL user:123
```

**List (Queue):**
```bash
# Push to queue
LPUSH notifications "New message"

# Pop from queue
RPOP notifications
```

**Set (Unique items):**
```bash
# Add to set
SADD online_users user:123 user:456 user:789

# Check membership
SISMEMBER online_users user:123

# Get all members
SMEMBERS online_users
```

## Monitoring

Redis metrics are exposed via the Redis Exporter and collected by Prometheus:

| Metric | Description |
|--------|-------------|
| `redis_memory_used_bytes` | Memory currently used |
| `redis_memory_max_bytes` | Maximum memory configured |
| `redis_connected_clients` | Number of connected clients |
| `redis_commands_total` | Total commands processed |
| `redis_keyspace_hits_total` | Cache hits |
| `redis_keyspace_misses_total` | Cache misses |
| `redis_evicted_keys_total` | Keys evicted due to memory pressure |

View in Grafana: **Dashboards → Infrastructure Overview**

## Backup & Recovery

See [`backups/README.md`](backups/README.md) for detailed backup procedures.

Quick backup:
```bash
# Run backup script
bash infrastructure/redis/backups/backup.sh
```

## Troubleshooting

### Connection Refused

```bash
# Check if Redis is running
docker compose -f compose.base.yml -f compose.infrastructure.yml ps redis

# Check Redis logs
docker compose -f compose.base.yml -f compose.infrastructure.yml logs redis
```

### Memory Issues

Check memory usage:
```bash
docker compose exec redis redis-cli -a $REDIS_PASSWORD INFO memory
```

If Redis is evicting keys frequently, consider:
- Increasing `maxmemory` in `compose.infrastructure.yml`
- Reviewing cache TTLs in application code
- Using more efficient data structures

### High Eviction Rate

```bash
# Check eviction stats
docker compose exec redis redis-cli -a $REDIS_PASSWORD INFO stats | grep evicted_keys
```

If evictions are high:
- Increase memory limit
- Reduce TTLs for less important data
- Review cache key patterns
