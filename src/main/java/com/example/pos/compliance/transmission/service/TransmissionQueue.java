package com.example.pos.compliance.transmission.service;

import java.util.UUID;

public interface TransmissionQueue {

    void enqueue(UUID transmissionId);

    UUID dequeue();

    int size();

    void clear();
}
