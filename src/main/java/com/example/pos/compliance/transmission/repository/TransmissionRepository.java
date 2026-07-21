package com.example.pos.compliance.transmission.repository;

import com.example.pos.compliance.transmission.model.Transmission;
import com.example.pos.compliance.transmission.model.TransmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransmissionRepository extends JpaRepository<Transmission, Long> {

    Optional<Transmission> findByInvoiceId(Long invoiceId);

    Optional<Transmission> findByIdempotencyKey(String idempotencyKey);

    List<Transmission> findByTransmissionStatus(TransmissionStatus status);

    List<Transmission> findByTransmissionStatusAndNextRetryTimeBefore(TransmissionStatus status, LocalDateTime now);

    long countByTransmissionStatus(TransmissionStatus status);

    long countByInvoiceIdInAndTransmissionStatus(List<Long> invoiceIds, TransmissionStatus status);
}
