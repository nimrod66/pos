package com.example.pos.procurement.supplierinvoices.service;

import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.procurement.supplierinvoices.dto.SupplierInvoiceRequestDto;
import com.example.pos.procurement.supplierinvoices.model.SupplierInvoices;
import com.example.pos.procurement.supplierinvoices.repository.SupplierInvoicesRepository;
import com.example.pos.procurement.suppliers.model.Suppliers;
import com.example.pos.procurement.suppliers.repository.SupplierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SupplierInvoicesService {

    private final SupplierInvoicesRepository repo;
    private final SupplierRepository supplierRepo;

    public SupplierInvoicesService(SupplierInvoicesRepository repo, SupplierRepository supplierRepo) {
        this.repo = repo;
        this.supplierRepo = supplierRepo;
    }

    public SupplierInvoices create(SupplierInvoiceRequestDto dto) {
        Suppliers supplier = supplierRepo.findById(dto.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", dto.getSupplierId()));
        SupplierInvoices si = new SupplierInvoices();
        si.setSuppliers(supplier);
        si.setInvoiceNumber(dto.getInvoiceNumber());
        si.setInvoiceDate(LocalDateTime.now());
        si.setSubTotal(dto.getSubTotal());
        si.setTax(dto.getTax() != null ? dto.getTax() : java.math.BigDecimal.ZERO);
        si.setTotal(dto.getTotal());
        si.setBalanceDue(dto.getTotal());
        si.setStatus(SupplierInvoices.Status.NOT_PAID);
        return repo.save(si);
    }

    @Transactional(readOnly = true)
    public Page<SupplierInvoices> getBySupplier(UUID supplierId, Pageable pageable) {
        List<SupplierInvoices> list = repo.findBySuppliers_Id(supplierId);
        return new PageImpl<>(list, pageable, list.size());
    }

    @Transactional(readOnly = true)
    public SupplierInvoices getById(UUID id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("SupplierInvoice", id));
    }
}
