package com.example.pos.finance.cashtransactions.repository;

import java.util.UUID;

import com.example.pos.finance.cashtransactions.model.CashTransactions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

import java.util.List;

public interface CashTransactionsRepository extends JpaRepository<CashTransactions, UUID> {

    List<CashTransactions> findByCashDrawersIdOrderByIdDesc(UUID cashDrawerId);

    @Query("select coalesce(sum(case "
            + "when transaction.transactionType = 'CASH_IN' then transaction.amount "
            + "when transaction.transactionType = 'CASH_OUT' then -transaction.amount "
            + "else 0 end), 0) from CashTransactions transaction "
            + "where transaction.cashDrawers.id = :drawerId")
    BigDecimal sumNetCashForDrawer(@Param("drawerId") UUID drawerId);
}
