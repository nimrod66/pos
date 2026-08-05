package com.example.pos.finance.cashtransactions.repository;

import java.util.UUID;

import com.example.pos.finance.cashtransactions.model.CashTransactions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CashTransactionsRepository extends JpaRepository<CashTransactions, UUID> {

    List<CashTransactions> findByCashDrawersIdOrderByIdDesc(UUID cashDrawerId);
}
