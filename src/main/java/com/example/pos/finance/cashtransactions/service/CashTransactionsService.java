package com.example.pos.finance.cashtransactions.service;

import com.example.pos.finance.cashtransactions.model.CashTransactions;
import com.example.pos.finance.cashtransactions.repository.CashTransactionsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CashTransactionsService {

    private final CashTransactionsRepository repo;

    public CashTransactionsService(CashTransactionsRepository repo) {
        this.repo = repo;
    }

    public List<CashTransactions> getByCashDrawer(Long cashDrawerId) {
        return repo.findByCashDrawersIdOrderByIdDesc(cashDrawerId);
    }
}
