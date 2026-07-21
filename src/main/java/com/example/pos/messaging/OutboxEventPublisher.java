package com.example.pos.messaging;

import com.example.pos.messaging.entity.OutboxEvent;
import com.example.pos.messaging.repository.OutboxEventRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxRepository;

    public OutboxEventPublisher(OutboxEventRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String aggregateType, String aggregateId, String eventType, String payload) {
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payload)
                .status(OutboxEvent.Status.PENDING)
                .build();
        outboxRepository.save(event);
    }
}
