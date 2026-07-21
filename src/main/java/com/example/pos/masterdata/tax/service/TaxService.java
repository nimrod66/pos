package com.example.pos.masterdata.tax.service;

import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.masterdata.tax.dto.TaxRequestDto;
import com.example.pos.masterdata.tax.model.Tax;
import com.example.pos.masterdata.tax.repository.TaxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TaxService {

    private final TaxRepository repository;

    public TaxService(TaxRepository repository) { this.repository = repository; }

    public Tax create(TaxRequestDto dto) {
        if (repository.existsByTaxName(dto.getTaxName()))
            throw new ConflictException("Tax '" + dto.getTaxName() + "' already exists");
        Tax tax = new Tax();
        tax.setTaxName(dto.getTaxName());
        tax.setTaxDescription(dto.getTaxDescription());
        tax.setTaxRate(dto.getTaxRate());
        return repository.save(tax);
    }

    @Transactional(readOnly = true)
    public List<Tax> getAll() { return repository.findAll(); }

    @Transactional(readOnly = true)
    public Tax getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tax", id));
    }

    public Tax update(Long id, TaxRequestDto dto) {
        Tax tax = getById(id);
        if (repository.existsByTaxNameAndIdNot(dto.getTaxName(), id))
            throw new ConflictException("Tax '" + dto.getTaxName() + "' already exists");
        tax.setTaxName(dto.getTaxName());
        tax.setTaxDescription(dto.getTaxDescription());
        tax.setTaxRate(dto.getTaxRate());
        return repository.save(tax);
    }

    public void delete(Long id) { repository.delete(getById(id)); }
}
