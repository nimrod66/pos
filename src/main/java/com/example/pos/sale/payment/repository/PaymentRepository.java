package com.example.pos.sale.payment.repository;

import java.util.UUID;

import com.example.pos.sale.payment.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findBySalesId(UUID saleId);

    Page<Payment> findBySalesId(UUID saleId, Pageable pageable);

    Optional<Payment> findByTransactionReference(String transactionReference);
}
