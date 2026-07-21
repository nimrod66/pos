package com.example.pos.sale.receipts.repository;

import com.example.pos.sale.receipts.model.Receipts;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReceiptsRepository extends JpaRepository<Receipts, Long> {

    List<Receipts> findBySalesId(Long saleId);
}
