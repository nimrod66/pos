package com.example.pos.customer.repository;

import com.example.pos.customer.model.CustomerTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CustomerTransactionRepository extends JpaRepository<CustomerTransaction, UUID> {

    List<CustomerTransaction> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    @Query("SELECT ct.runningBalance FROM CustomerTransaction ct WHERE ct.customer.id = :customerId ORDER BY ct.createdAt DESC LIMIT 1")
    BigDecimal findLatestBalance(@Param("customerId") UUID customerId);
}
