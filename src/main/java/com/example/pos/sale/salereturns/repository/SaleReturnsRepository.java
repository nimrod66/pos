package com.example.pos.sale.salereturns.repository;

import com.example.pos.sale.salereturns.model.SaleReturns;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaleReturnsRepository extends JpaRepository<SaleReturns, Long> {

    List<SaleReturns> findBySalesId(Long saleId);

    Page<SaleReturns> findBySalesId(Long saleId, Pageable pageable);
}
