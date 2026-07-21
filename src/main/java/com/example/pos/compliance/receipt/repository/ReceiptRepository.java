package com.example.pos.compliance.receipt.repository;

import com.example.pos.compliance.receipt.model.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    Optional<Receipt> findByReceiptNumber(String receiptNumber);

    Optional<Receipt> findBySaleId(Long saleId);
}
