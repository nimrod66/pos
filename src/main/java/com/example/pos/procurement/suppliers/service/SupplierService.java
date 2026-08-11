package com.example.pos.procurement.suppliers.service;

import com.example.pos.common.annotation.Auditable;
import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.procurement.suppliers.dto.SupplierRequestDto;
import com.example.pos.procurement.suppliers.model.Suppliers;
import com.example.pos.procurement.suppliers.repository.SupplierRepository;
import com.example.pos.security.auth.AuthenticatedUserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class SupplierService {

    private final SupplierRepository repo;
    private final AuthenticatedUserContext current;

    public SupplierService(SupplierRepository repo, AuthenticatedUserContext current) {
        this.repo = repo;
        this.current = current;
    }

    @Auditable(action = "CREATE_SUPPLIER", entity = "Supplier")
    public Suppliers create(SupplierRequestDto dto) {
        UUID pharmacyId = current.pharmacy().getId();
        if (repo.existsByPharmacyIdAndSupplierNameIgnoreCase(pharmacyId, dto.getSupplierName().trim()))
            throw new ConflictException("Supplier '" + dto.getSupplierName() + "' already exists");
        Suppliers s = new Suppliers();
        s.setPharmacy(current.pharmacy());
        map(dto, s);
        if (dto.getStatus() == null) s.setStatus(Suppliers.Status.ACTIVE);
        return repo.save(s);
    }

    @Transactional(readOnly = true)
    public Page<Suppliers> getAll(Pageable pageable) {
        return repo.findByPharmacyId(current.pharmacy().getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Suppliers> search(String q, Pageable pageable) {
        return repo.searchByPharmacy(current.pharmacy().getId(), q.trim(), pageable);
    }

    @Transactional(readOnly = true)
    public Suppliers getById(UUID id) {
        return repo.findByIdAndPharmacyId(id, current.pharmacy().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", id));
    }

    @Auditable(action = "UPDATE_SUPPLIER", entity = "Supplier")
    public Suppliers update(UUID id, SupplierRequestDto dto) {
        Suppliers s = getById(id);
        if (repo.existsByPharmacyIdAndSupplierNameIgnoreCaseAndIdNot(
                current.pharmacy().getId(), dto.getSupplierName().trim(), id))
            throw new ConflictException("Supplier '" + dto.getSupplierName() + "' already exists");
        map(dto, s);
        return repo.save(s);
    }

    @Auditable(action = "DELETE_SUPPLIER", entity = "Supplier")
    public void delete(UUID id) {
        Suppliers supplier = getById(id);
        supplier.setStatus(Suppliers.Status.INACTIVE);
        repo.save(supplier);
    }

    private void map(SupplierRequestDto dto, Suppliers s) {
        s.setSupplierName(dto.getSupplierName().trim());
        s.setLicenseNumber(dto.getLicenseNumber());
        s.setPhoneNumber(dto.getPhoneNumber());
        s.setAddress(dto.getAddress());
        s.setEmail(dto.getEmail());
        s.setContactPerson(dto.getContactPerson());
        s.setPaymentTerms(dto.getPaymentTerms());
        if (dto.getStatus() != null) {
            try {
                s.setStatus(Suppliers.Status.valueOf(dto.getStatus().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid supplier status: " + dto.getStatus());
            }
        }
    }
}
