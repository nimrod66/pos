package com.example.pos.compliance.transmission.service;

import com.example.pos.compliance.transmission.model.Transmission;
import com.example.pos.compliance.transmission.model.TransmissionStatus;

public interface TransmissionQueue {

    void enqueue(Long transmissionId);

    Long dequeue();

    int size();

    void clear();
}
