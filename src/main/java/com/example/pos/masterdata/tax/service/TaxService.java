package com.example.pos.masterdata.tax.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.masterdata.tax.dto.TaxRequestDto;
import com.example.pos.masterdata.tax.model.Tax;
import com.example.pos.masterdata.tax.repository.TaxRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TaxService {

    private final TaxRepository repository;

    public TaxService(TaxRepository repository) { this.repository = repository; }

    public Tax create(TaxRequestDto dto) {
        if (repository.existsByCode(dto.getCode()))
            throw new ConflictException("Tax code '" + dto.getCode() + "' already exists");
        if (repository.existsByTaxName(dto.getTaxName()))
            throw new ConflictException("Tax '" + dto.getTaxName() + "' already exists");
        Tax tax = new Tax();
        tax.setCode(dto.getCode());
        tax.setTaxName(dto.getTaxName());
        tax.setTaxDescription(dto.getTaxDescription());
        tax.setTaxRate(dto.getTaxRate());
        tax.setTaxType(dto.getTaxType());
        tax.setActive(dto.isActive());
        return repository.save(tax);
    }

    @Transactional(readOnly = true)
    public Page<Tax> getAll(Pageable pageable) { return repository.findAll(pageable); }

    @Transactional(readOnly = true)
    public Page<Tax> getActive(Pageable pageable) { return repository.findByActiveTrue(pageable); }

    @Transactional(readOnly = true)
    public Tax getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tax", id));
    }

    @Transactional(readOnly = true)
    public Tax getByCode(String code) {
        return repository.findByCode(code).orElseThrow(() -> new ResourceNotFoundException("Tax code", code));
    }

    public Tax update(Long id, TaxRequestDto dto) {
        Tax tax = getById(id);
        if (repository.existsByCodeAndIdNot(dto.getCode(), id))
            throw new ConflictException("Tax code '" + dto.getCode() + "' already exists");
        if (repository.existsByTaxNameAndIdNot(dto.getTaxName(), id))
            throw new ConflictException("Tax '" + dto.getTaxName() + "' already exists");
        tax.setCode(dto.getCode());
        tax.setTaxName(dto.getTaxName());
        tax.setTaxDescription(dto.getTaxDescription());
        tax.setTaxRate(dto.getTaxRate());
        tax.setTaxType(dto.getTaxType());
        tax.setActive(dto.isActive());
        return repository.save(tax);
    }

    public Tax toggleActive(Long id) {
        Tax tax = getById(id);
        tax.setActive(!tax.isActive());
        return repository.save(tax);
    }

    public void delete(Long id) {
        Tax tax = getById(id);
        if (tax.getMedicine() != null && !tax.getMedicine().isEmpty()) {
            throw new BadRequestException("Cannot delete tax category assigned to " + tax.getMedicine().size() + " medicines");
        }
        repository.delete(tax);
    }
}
