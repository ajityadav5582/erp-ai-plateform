# Kafka Backup and Recovery

## Overview

This document describes backup and recovery procedures for Kafka in the ERP AI Platform.

## Backup Strategy

### 1. Topic Configuration Backup

Export topic configurations for disaster recovery:

```bash
# List all topics
kafka-topics.sh --bootstrap-server kafka:9092 --list > kafka-topics.txt

# Export topic configurations
kafka-topics.sh --bootstrap-server kafka:9092 --describe > kafka-topic-configs.txt
```

### 2. Data Backup

For development environments, Kafka data is persisted in Docker volumes. For production, use MirrorMaker 2.0 or Confluent Replicator.

```bash
# Create backup directory
mkdir -p backups/$(date +%Y%m%d_%H%M%S)

# Backup Kafka data volume
docker run --rm \
  -v erpai_kafka_data:/data \
  -v $(pwd)/backups:/backup \
  alpine tar czf /backup/kafka-data-$(date +%Y%m%d_%H%M%S).tar.gz -C /data .
```

### 3. Schema Registry Backup

```bash
# Export all schemas
curl -X GET http://localhost:8081/subjects > schemas.json

# Export schema compatibility settings
curl -X GET http://localhost:8081/config > schema-config.json
```

## Recovery Procedures

### 1. Restore Topic Configuration

```bash
# Recreate topics from configuration
while read topic; do
  kafka-topics.sh --bootstrap-server kafka:9092 --create --topic "$topic" --partitions 12 --replication-factor 1
done < kafka-topics.txt
```

### 2. Restore Data

```bash
# Restore Kafka data volume
docker run --rm \
  -v erpai_kafka_data:/data \
  -v $(pwd)/backups:/backup \
  alpine tar xzf /backup/kafka-data-20240101_120000.tar.gz -C /data
```

## Automated Backup Script

```bash
#!/bin/bash
# backup.sh - Kafka backup script

BACKUP_DIR="./backups"
DATE=$(date +%Y%m%d_%H%M%S)

# Create backup directory
mkdir -p ${BACKUP_DIR}/${DATE}

# Backup topic list
kafka-topics.sh --bootstrap-server kafka:9092 --list > ${BACKUP_DIR}/${DATE}/topics.txt

# Backup topic configurations
kafka-topics.sh --bootstrap-server kafka:9092 --describe > ${BACKUP_DIR}/${DATE}/topic-configs.txt

# Backup schema registry
curl -s http://localhost:8081/subjects > ${BACKUP_DIR}/${DATE}/schemas.json

# Backup Kafka data (if needed)
docker run --rm \
  -v erpai_kafka_data:/data \
  -v $(pwd)/backups:/backup \
  alpine tar czf /backup/kafka-data-${DATE}.tar.gz -C /data .

echo "Backup completed: ${BACKUP_DIR}/${DATE}"
```

## Disaster Recovery

### 1. Complete Kafka Failure

1. Stop all services consuming from Kafka
2. Restore Kafka data from backup
3. Recreate topic configurations
4. Start Kafka
5. Verify topic availability
6. Restart consumer services

### 2. Schema Registry Failure

1. Restore schema registry data
2. Reimport schemas from backup
3. Verify schema compatibility

## Monitoring

### Backup Health Checks

```bash
# Check backup directory exists
if [ ! -d "./backups" ]; then
  echo "ERROR: Backup directory not found"
  exit 1
fi

# Check latest backup
LATEST_BACKUP=$(ls -t ./backups | head -1)
if [ -z "$LATEST_BACKUP" ]; then
  echo "ERROR: No backups found"
  exit 1
fi

echo "Latest backup: $LATEST_BACKUP"
```

## Retention Policy

- **Regular topics:** 7 days (604800000 ms)
- **DLQ topics:** 30 days (2592000000 ms)
- **Audit topics:** 7 days (604800000 ms)

## Best Practices

- [ ] Schedule daily backups
- [ ] Test restore procedures regularly
- [ ] Monitor backup disk space
- [ ] Encrypt backup files
- [ ] Store backups in multiple locations
- [ ] Document recovery procedures
- [ ] Practice disaster recovery drills
