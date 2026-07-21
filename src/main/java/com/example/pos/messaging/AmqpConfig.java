package com.example.pos.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AmqpConfig {

    public static final String EXCHANGE = "pharmacy.events";

    public static final String QUEUE_NOTIFICATIONS = "notifications.queue";
    public static final String QUEUE_AUDIT = "audit.queue";
    public static final String QUEUE_ETIMS = "etims.queue";

    @Bean
    public TopicExchange pharmacyExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue notificationsQueue() { return new Queue(QUEUE_NOTIFICATIONS, true); }

    @Bean
    public Queue auditQueue() { return new Queue(QUEUE_AUDIT, true); }

    @Bean
    public Queue etimsQueue() { return new Queue(QUEUE_ETIMS, true); }

    @Bean
    public Binding notificationsBinding() {
        return BindingBuilder.bind(notificationsQueue()).to(pharmacyExchange())
                .with("stock.*");
    }

    @Bean
    public Binding auditBinding() {
        return BindingBuilder.bind(auditQueue()).to(pharmacyExchange())
                .with("entity.#");
    }

    @Bean
    public Binding etimsBinding() {
        return BindingBuilder.bind(etimsQueue()).to(pharmacyExchange())
                .with("sale.completed");
    }
}
