package com.example.pos.sale.payment.repository;

import com.example.pos.sale.payment.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findBySalesId(Long saleId);

    Page<Payment> findBySalesId(Long saleId, Pageable pageable);

    Optional<Payment> findByTransactionReference(String transactionReference);
}
