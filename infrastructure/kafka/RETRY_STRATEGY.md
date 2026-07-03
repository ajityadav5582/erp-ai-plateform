# Kafka Retry Strategy

## 1. Purpose

This document defines the retry strategy for Kafka event processing in the ERP AI Platform. The strategy ensures reliable message delivery while preventing system overload.

## 2. Retry Pattern

### 2.1 Exponential Backoff

```java
// Retry configuration
RetryPolicy retryPolicy = RetryPolicy.builder()
    .initialDelay(Duration.ofSeconds(1))
    .maxDelay(Duration.ofSeconds(60))
    .multiplier(2.0)
    .maxAttempts(5)
    .retryOn(Exception.class)
    .build();
```

### 2.2 Retry Configuration

| Attempt | Delay | Description |
|---------|-------|-------------|
| 1 | 1 second | Initial retry |
| 2 | 2 seconds | First backoff |
| 3 | 4 seconds | Second backoff |
| 4 | 8 seconds | Third backoff |
| 5 | 16 seconds | Final retry |

### 2.3 Retry Topic Pattern

```
{environment}.retry.{category}
```

**Examples:**
- `dev.retry.events`
- `dev.retry.commands`

## 3. Implementation

### 3.1 Spring Kafka Configuration

```java
@Configuration
@EnableKafka
public class KafkaRetryConfiguration {
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory factory = new ConcurrentKafkaListenerContainerFactory();
        factory.setConsumerFactory(consumerFactory());
        
        // Retry configuration
        factory.setCommonErrorHandler(new DefaultErrorHandler(
            new KafkaTopicBasedDltPublishingRecoverer(kafkaTemplate),
            new FixedBackOff(1000L, 5) // 1 second initial, 5 attempts
        ));
        
        return factory;
    }
    
    @Bean
    public RetryTemplate retryTemplate() {
        return RetryTemplate.builder()
            .maxAttempts(5)
            .exponentialBackoff(1000, 2.0, 60000)
            .retryOn(Exception.class)
            .build();
    }
}
```

### 3.2 Retry with Dead Letter Queue

```java
@Component
public class EventProcessor {
    
    private static final String RETRY_TOPIC = "dev.retry.events";
    private static final String DLQ_TOPIC = "dev.dlq.events";
    
    @KafkaListener(
        topics = "dev.tenant.invoice.invoice-created",
        groupId = "invoice-service.processor.main"
    )
    public void handle(ConsumerRecord<String, DomainEvent> record) {
        try {
            processEvent(record.value());
        } catch (TransientException e) {
            // Send to retry topic with delay header
            sendToRetry(record, calculateDelay(record.headers()));
        } catch (PermanentException e) {
            // Send to DLQ immediately
            sendToDlq(record, e);
        }
    }
    
    private void sendToRetry(ConsumerRecord<String, DomainEvent> record, long delayMs) {
        ProducerRecord<String, DomainEvent> retryRecord = new ProducerRecord<>(
            RETRY_TOPIC,
            record.key(),
            record.value()
        );
        retryRecord.headers().add("retry-count", String.valueOf(getRetryCount(record) + 1).getBytes());
        retryRecord.headers().add("retry-delay", String.valueOf(delayMs).getBytes());
        retryRecord.headers().add("original-topic", record.topic().getBytes());
        
        kafkaTemplate.send(retryRecord);
    }
}
```

### 3.3 Retry Topic Consumer

```java
@Component
public class RetryEventProcessor {
    
    @KafkaListener(
        topics = "dev.retry.events",
        groupId = "retry-processor.main"
    )
    public void handleRetry(ConsumerRecord<String, DomainEvent> record) {
        int retryCount = getRetryCount(record);
        long delayMs = getRetryDelay(record);
        
        if (retryCount >= 5) {
            // Max retries exceeded, send to DLQ
            sendToDlq(record, new MaxRetriesExceededException("Max retries exceeded"));
            return;
        }
        
        // Check if delay has passed
        if (shouldDelay(record)) {
            // Re-queue for later processing
            reprocessWithDelay(record, calculateNextDelay(retryCount));
            return;
        }
        
        // Forward to original topic
        String originalTopic = getOriginalTopic(record);
        kafkaTemplate.send(new ProducerRecord<>(originalTopic, record.key(), record.value()));
    }
}
```

## 4. Retry Headers

| Header | Type | Description |
|--------|------|-------------|
| `retry-count` | Integer | Current retry attempt number |
| `retry-delay` | Long | Delay in milliseconds before next retry |
| `original-topic` | String | Original topic for forwarding |
| `error-message` | String | Error message from last failure |
| `error-timestamp` | Long | Timestamp of last error |

## 5. Retry Topic Configuration

```bash
# Create retry topic with short retention
kafka-topics.sh --bootstrap-server kafka:9092 \
    --create \
    --topic dev.retry.events \
    --partitions 12 \
    --replication-factor 1 \
    --config retention.ms=86400000 \
    --config cleanup.policy=delete
```

## 6. Monitoring

### 6.1 Key Metrics

- **Retry rate:** Number of messages sent to retry topics
- **Retry depth:** Number of messages waiting in retry topics
- **Retry success rate:** Percentage of retried messages that succeed
- **Average retry count:** Average number of retries per message

### 6.2 Alerts

- Retry depth > 1000: Warning
- Retry rate > 100/min: Warning
- Retry success rate < 50%: Alert

## 7. Best Practices

- [ ] Use exponential backoff to prevent thundering herd
- [ ] Set maximum retry attempts to prevent infinite loops
- [ ] Distinguish between transient and permanent errors
- [ ] Log retry attempts for debugging
- [ ] Monitor retry metrics
- [ ] Alert on high retry rates
- [ ] Clean up old retry messages
- [ ] Use retry headers for tracking
