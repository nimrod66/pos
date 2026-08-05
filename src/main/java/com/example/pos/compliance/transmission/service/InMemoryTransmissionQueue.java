package com.example.pos.compliance.transmission.service;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class InMemoryTransmissionQueue implements TransmissionQueue {

    private final ConcurrentLinkedQueue<UUID> queue = new ConcurrentLinkedQueue<>();

    @Override
    public void enqueue(UUID transmissionId) {
        queue.add(transmissionId);
    }

    @Override
    public UUID dequeue() {
        return queue.poll();
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public void clear() {
        queue.clear();
    }
}
