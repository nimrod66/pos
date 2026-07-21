package com.example.pos.finance.cashtransactions.repository;

import com.example.pos.finance.cashtransactions.model.CashTransactions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CashTransactionsRepository extends JpaRepository<CashTransactions, Long> {

    List<CashTransactions> findByCashDrawersIdOrderByIdDesc(Long cashDrawerId);
}
