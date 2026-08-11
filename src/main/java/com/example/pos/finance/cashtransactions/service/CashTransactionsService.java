package com.example.pos.finance.cashtransactions.service;

import com.example.pos.finance.cashtransactions.model.CashTransactions;
import com.example.pos.finance.cashtransactions.repository.CashTransactionsRepository;
import com.example.pos.finance.cashdrawers.service.CashDrawersService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CashTransactionsService {

    private final CashTransactionsRepository repo;
    private final CashDrawersService drawersService;

    public CashTransactionsService(CashTransactionsRepository repo,
                                   CashDrawersService drawersService) {
        this.repo = repo;
        this.drawersService = drawersService;
    }

    public Page<CashTransactions> getByCashDrawer(UUID cashDrawerId, Pageable pageable) {
        drawersService.getById(cashDrawerId);
        List<CashTransactions> list = repo.findByCashDrawersIdOrderByIdDesc(cashDrawerId);
        return new PageImpl<>(list, pageable, list.size());
    }
}
