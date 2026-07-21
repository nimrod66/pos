package com.example.pos.compliance.transmission.repository;

import com.example.pos.compliance.transmission.model.TransmissionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransmissionAttemptRepository extends JpaRepository<TransmissionAttempt, Long> {

    List<TransmissionAttempt> findByTransmissionIdOrderByAttemptNumberDesc(Long transmissionId);
}
