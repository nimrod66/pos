package com.example.pos.messaging;

import com.example.pos.messaging.entity.OutboxEvent;
import com.example.pos.messaging.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);
    private final OutboxEventRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxPoller(OutboxEventRepository outboxRepository, RabbitTemplate rabbitTemplate) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> pendingEvents = outboxRepository
                .findByStatusOrderByCreatedAtAsc(OutboxEvent.Status.PENDING, PageRequest.of(0, 100));

        for (OutboxEvent event : pendingEvents) {
            try {
                String routingKey = event.getAggregateType().toLowerCase() + "."
                        + event.getEventType().toLowerCase();

                MessageProperties props = new MessageProperties();
                props.setHeader("aggregateType", event.getAggregateType());
                props.setHeader("aggregateId", event.getAggregateId());
                props.setHeader("eventType", event.getEventType());
                props.setHeader("messageId", event.getId().toString());

                byte[] body = event.getPayload() != null
                        ? event.getPayload().getBytes(java.nio.charset.StandardCharsets.UTF_8)
                        : "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8);

                Message message = new Message(body, props);
                rabbitTemplate.send(AmqpConfig.EXCHANGE, routingKey, message);

                event.setStatus(OutboxEvent.Status.PUBLISHED);
                event.setPublishedAt(LocalDateTime.now());
                log.debug("Published to {} with key {}: {}", AmqpConfig.EXCHANGE, routingKey, event.getEventType());
            } catch (Exception e) {
                log.error("Failed to publish event {}: {}", event.getId(), e.getMessage());
                event.setStatus(OutboxEvent.Status.FAILED);
            }
            outboxRepository.save(event);
        }
    }
}
