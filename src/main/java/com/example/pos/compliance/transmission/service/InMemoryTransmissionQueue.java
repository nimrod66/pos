package com.example.pos.compliance.transmission.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class InMemoryTransmissionQueue implements TransmissionQueue {

    private final ConcurrentLinkedQueue<Long> queue = new ConcurrentLinkedQueue<>();

    @Override
    public void enqueue(Long transmissionId) {
        queue.add(transmissionId);
    }

    @Override
    public Long dequeue() {
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
