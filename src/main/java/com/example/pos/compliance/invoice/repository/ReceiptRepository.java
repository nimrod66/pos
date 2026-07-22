package com.example.pos.compliance.invoice.repository;

import com.example.pos.compliance.invoice.model.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    Optional<Receipt> findByReceiptNumber(String receiptNumber);

    Optional<Receipt> findBySaleId(Long saleId);
}
