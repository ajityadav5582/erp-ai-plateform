package com.erp.business.pos.integration;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class PosSalesEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PosSalesEventPublisher.class);
    public static final String TOPIC = "pos.transaction.completed";
    public static final String SERVICE_TOKEN_HEADER = "X-Service-Token";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String serviceToken;

    public PosSalesEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                 @Value("${erp.integration.service-token}") String serviceToken) {
        this.kafkaTemplate = kafkaTemplate;
        this.serviceToken = serviceToken;
    }

    public void publish(PosTransactionCompletedEvent event) {
        try {
            ProducerRecord<String, Object> record = new ProducerRecord<>(TOPIC, String.valueOf(event.posTransactionId()), event);
            record.headers().add(new RecordHeader(SERVICE_TOKEN_HEADER, serviceToken.getBytes(StandardCharsets.UTF_8)));
            kafkaTemplate.send(record);
        } catch (Exception ex) {
            log.warn("Failed to publish POS transaction completed event {}: {}", event.posTransactionId(), ex.getMessage());
        }
    }
}
