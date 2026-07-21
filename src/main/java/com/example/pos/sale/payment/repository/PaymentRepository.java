package com.example.pos.sale.payment.repository;

import com.example.pos.sale.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findBySalesId(Long saleId);

    Optional<Payment> findByTransactionReference(String transactionReference);
}
