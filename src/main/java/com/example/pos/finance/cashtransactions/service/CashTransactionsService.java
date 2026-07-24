package com.example.pos.finance.cashtransactions.service;

import com.example.pos.finance.cashtransactions.model.CashTransactions;
import com.example.pos.finance.cashtransactions.repository.CashTransactionsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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

    public Page<CashTransactions> getByCashDrawer(Long cashDrawerId, Pageable pageable) {
        List<CashTransactions> list = repo.findByCashDrawersIdOrderByIdDesc(cashDrawerId);
        return new PageImpl<>(list, pageable, list.size());
    }
}
