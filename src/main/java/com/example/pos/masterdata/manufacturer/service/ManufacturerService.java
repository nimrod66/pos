package com.example.pos.masterdata.manufacturer.service;

import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.masterdata.manufacturer.dto.ManufacturerRequestDto;
import com.example.pos.masterdata.manufacturer.model.Manufacturer;
import com.example.pos.masterdata.manufacturer.repository.ManufacturerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ManufacturerService {

    private final ManufacturerRepository repository;

    public ManufacturerService(ManufacturerRepository repository) { this.repository = repository; }

    public Manufacturer create(ManufacturerRequestDto dto) {
        if (repository.existsByManufacturerName(dto.getManufacturerName()))
            throw new ConflictException("Manufacturer '" + dto.getManufacturerName() + "' already exists");
        Manufacturer m = new Manufacturer();
        m.setManufacturerName(dto.getManufacturerName());
        m.setManufacturerCountry(dto.getManufacturerCountry());
        m.setManufacturerContact(dto.getManufacturerContact());
        return repository.save(m);
    }

    @Transactional(readOnly = true)
    public Page<Manufacturer> getAll(Pageable pageable) { return repository.findAll(pageable); }

    @Transactional(readOnly = true)
    public Manufacturer getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Manufacturer", id));
    }

    public Manufacturer update(Long id, ManufacturerRequestDto dto) {
        Manufacturer m = getById(id);
        if (repository.existsByManufacturerNameAndIdNot(dto.getManufacturerName(), id))
            throw new ConflictException("Manufacturer '" + dto.getManufacturerName() + "' already exists");
        m.setManufacturerName(dto.getManufacturerName());
        m.setManufacturerCountry(dto.getManufacturerCountry());
        m.setManufacturerContact(dto.getManufacturerContact());
        return repository.save(m);
    }

    public void delete(Long id) { repository.delete(getById(id)); }
}
