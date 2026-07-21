package com.example.pos.compliance.etims.service;

import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.compliance.etims.model.Etims;
import com.example.pos.compliance.etims.repository.EtimsRepository;
import com.example.pos.sale.sales.model.Sales;
import com.example.pos.sale.sales.repository.SalesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EtimsService {

    private final EtimsRepository repo;
    private final SalesRepository salesRepo;

    public EtimsService(EtimsRepository repo, SalesRepository salesRepo) { this.repo = repo; this.salesRepo = salesRepo; }

    public Etims submit(Long saleId, String qrCode, String status) {
        Sales sale = salesRepo.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", saleId));
        Etims e = new Etims();
        e.setSales(sale);
        e.setSubmissionStatus(status != null ? status : "SUBMITTED");
        e.setQrCode(qrCode);
        return repo.save(e);
    }

    @Transactional(readOnly = true)
    public List<Etims> getBySale(Long saleId) { return repo.findBySalesId(saleId); }
}
