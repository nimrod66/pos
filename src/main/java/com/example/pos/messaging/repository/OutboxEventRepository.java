package com.example.pos.messaging.repository;

import java.util.UUID;

import com.example.pos.messaging.entity.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEvent.Status status, Pageable pageable);
}
