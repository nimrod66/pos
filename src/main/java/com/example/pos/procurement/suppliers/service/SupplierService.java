package com.example.pos.procurement.suppliers.service;

import com.example.pos.common.annotation.Auditable;
import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.procurement.suppliers.dto.SupplierRequestDto;
import com.example.pos.procurement.suppliers.model.Suppliers;
import com.example.pos.procurement.suppliers.repository.SupplierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class SupplierService {

    private final SupplierRepository repo;
    public SupplierService(SupplierRepository repo) { this.repo = repo; }

    @Auditable(action = "CREATE_SUPPLIER", entity = "Supplier")
    public Suppliers create(SupplierRequestDto dto) {
        if (repo.existsBySupplierName(dto.getSupplierName()))
            throw new ConflictException("Supplier '" + dto.getSupplierName() + "' already exists");
        Suppliers s = new Suppliers();
        map(dto, s);
        if (dto.getStatus() == null) s.setStatus(Suppliers.Status.ACTIVE);
        return repo.save(s);
    }

    @Transactional(readOnly = true)
    public Page<Suppliers> getAll(Pageable pageable) { return repo.findAll(pageable); }

    @Transactional(readOnly = true)
    public Page<Suppliers> search(String q, Pageable pageable) { return repo.search(q, pageable); }

    @Transactional(readOnly = true)
    public Suppliers getById(UUID id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Supplier", id));
    }

    @Auditable(action = "UPDATE_SUPPLIER", entity = "Supplier")
    public Suppliers update(UUID id, SupplierRequestDto dto) {
        Suppliers s = getById(id);
        if (repo.existsBySupplierNameAndIdNot(dto.getSupplierName(), id))
            throw new ConflictException("Supplier '" + dto.getSupplierName() + "' already exists");
        map(dto, s);
        return repo.save(s);
    }

    @Auditable(action = "DELETE_SUPPLIER", entity = "Supplier")
    public void delete(UUID id) { repo.delete(getById(id)); }

    private void map(SupplierRequestDto dto, Suppliers s) {
        s.setSupplierName(dto.getSupplierName());
        s.setLicenseNumber(dto.getLicenseNumber());
        s.setPhoneNumber(dto.getPhoneNumber());
        s.setAddress(dto.getAddress());
        s.setEmail(dto.getEmail());
        s.setContactPerson(dto.getContactPerson());
        s.setPaymentTerms(dto.getPaymentTerms());
        if (dto.getStatus() != null) s.setStatus(Suppliers.Status.valueOf(dto.getStatus().toUpperCase()));
    }
}
