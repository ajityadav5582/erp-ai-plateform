# Dead Letter Queue (DLQ) Strategy

## 1. Purpose

This document defines the Dead Letter Queue (DLQ) strategy for Kafka event processing in the ERP AI Platform. The DLQ ensures that failed messages are not lost and can be analyzed and reprocessed.

## 2. DLQ Pattern

### 2.1 When to Use DLQ

Messages are sent to DLQ when:

1. **Max retries exceeded:** Message has been retried the maximum number of times
2. **Permanent error:** Error is not recoverable (e.g., data validation error)
3. **Schema error:** Message doesn't match expected schema
4. **Deserialization error:** Message cannot be deserialized
5. **Business logic error:** Error in business logic that cannot be recovered

### 2.2 DLQ Topic Pattern

```
{environment}.dlq.{category}
```

**Examples:**
- `dev.dlq.events`
- `dev.dlq.commands`

## 3. Implementation

### 3.1 Spring Kafka Configuration

```java
@Configuration
public class KafkaDlqConfiguration {
    
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        // DLQ topic suffix
        String dlqTopicSuffix = ".dlq";
        
        // Create DLQ topic if not exists
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (record, ex) -> new TopicPartition(
                record.topic() + dlqTopicSuffix,
                record.partition()
            )
        );
        
        // Exponential backoff for retries
        BackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxInterval(60000L);
        
        return new DefaultErrorHandler(
            recoverer,
            backOff
        );
    }
}
```

### 3.2 DLQ Message Format

```java
public record DlqMessage<T>(
    String originalTopic,
    Integer partition,
    Long offset,
    String key,
    T value,
    String errorMessage,
    String errorClass,
    Instant failedAt,
    List<RetryAttempt> retryHistory
) {
    public record RetryAttempt(
        Integer attempt,
        String errorMessage,
        Instant failedAt
    ) {}
}
```

### 3.3 DLQ Producer

```java
@Component
public class DlqProducer {
    
    private static final String DLQ_TOPIC = "dev.dlq.events";
    
    @Autowired
    private KafkaTemplate<String, DlqMessage> kafkaTemplate;
    
    public void sendToDlq(ConsumerRecord<String, ?> record, Exception error) {
        DlqMessage<Object> dlqMessage = DlqMessage.builder()
            .originalTopic(record.topic())
            .partition(record.partition())
            .offset(record.offset())
            .key(record.key())
            .value(record.value())
            .errorMessage(error.getMessage())
            .errorClass(error.getClass().getSimpleName())
            .failedAt(Instant.now())
            .retryHistory(getRetryHistory(record))
            .build();
        
        kafkaTemplate.send(DLQ_TOPIC, record.key(), dlqMessage);
    }
}
```

### 3.4 DLQ Consumer

```java
@Component
public class DlqConsumer {
    
    @KafkaListener(
        topics = "dev.dlq.events",
        groupId = "dlq-processor.main"
    )
    public void handleDlq(ConsumerRecord<String, DlqMessage> record) {
        DlqMessage message = record.value();
        
        // Log for analysis
        log.warn("DLQ message received: topic={}, error={}", 
            message.originalTopic(), 
            message.errorMessage()
        );
        
        // Send to monitoring system
        alertService.sendAlert(
            "DLQ_MESSAGE",
            "Message sent to DLQ: " + message.originalTopic(),
            message
        );
        
        // Optionally reprocess
        if (shouldReprocess(message)) {
            reprocessMessage(message);
        }
    }
}
```

## 4. DLQ Topic Configuration

```bash
# Create DLQ topic with extended retention
kafka-topics.sh --bootstrap-server kafka:9092 \
    --create \
    --topic dev.dlq.events \
    --partitions 12 \
    --replication-factor 1 \
    --config retention.ms=2592000000 \
    --config cleanup.policy=delete \
    --config delete.retention.ms=86400000
```

## 5. DLQ Message Headers

| Header | Type | Description |
|--------|------|-------------|
| `original-topic` | String | Original topic the message came from |
| `original-partition` | Integer | Original partition |
| `original-offset` | Long | Original offset |
| `error-message` | String | Error message |
| `error-class` | String | Exception class name |
| `failed-at` | Long | Timestamp of failure |
| `retry-count` | Integer | Number of retry attempts |

## 6. Monitoring

### 6.1 Key Metrics

- **DLQ depth:** Number of messages in DLQ
- **DLQ rate:** Messages per second sent to DLQ
- **DLQ by topic:** Messages grouped by original topic
- **DLQ by error type:** Messages grouped by error class

### 6.2 Alerts

- DLQ depth > 100: Warning
- DLQ depth > 1000: Alert on-call team
- DLQ rate > 10/min: Warning
- DLQ rate > 100/min: Alert on-call team

## 7. DLQ Management

### 7.1 Reprocessing

```java
@Service
public class DlqReprocessor {
    
    public void reprocess(String messageId, String targetTopic) {
        // Fetch message from DLQ
        ConsumerRecord<String, DlqMessage> record = dlqRepository.findById(messageId);
        
        // Send to target topic
        kafkaTemplate.send(new ProducerRecord<>(
            targetTopic,
            record.value().key(),
            record.value().value()
        ));
        
        // Remove from DLQ
        dlqRepository.deleteById(messageId);
    }
}
```

### 7.2 Dead Letter Queue CLI

```bash
# View DLQ messages
kafka-console-consumer.sh --bootstrap-server kafka:9092 \
    --topic dev.dlq.events \
    --from-beginning

# Count DLQ messages
kafka-run-class.sh kafka.tools.GetOffsetShell \
    --bootstrap-server kafka:9092 \
    --topic dev.dlq.events \
    --time -1
```

## 8. Best Practices

- [ ] Always send failed messages to DLQ
- [ ] Include original message and error details
- [ ] Use extended retention for DLQ topics
- [ ] Monitor DLQ depth and rate
- [ ] Alert on high DLQ volume
- [ ] Provide reprocessing capability
- [ ] Log DLQ messages for analysis
- [ ] Document common DLQ patterns
- [ ] Regular DLQ cleanup (after analysis)
- [ ] Use DLQ for debugging and testing
