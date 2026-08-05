package com.example.pos.sale.salereturns.repository;

import java.util.UUID;

import com.example.pos.sale.salereturns.model.SaleReturns;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaleReturnsRepository extends JpaRepository<SaleReturns, UUID> {

    List<SaleReturns> findBySalesId(UUID saleId);

    Page<SaleReturns> findBySalesId(UUID saleId, Pageable pageable);
}
