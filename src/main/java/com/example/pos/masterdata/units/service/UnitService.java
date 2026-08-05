package com.example.pos.masterdata.units.service;

import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.masterdata.units.dto.UnitRequestDto;
import com.example.pos.masterdata.units.model.Unit;
import com.example.pos.masterdata.units.repository.UnitRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class UnitService {

    private final UnitRepository repository;

    public UnitService(UnitRepository repository) { this.repository = repository; }

    public Unit create(UnitRequestDto dto) {
        if (repository.existsByUnitName(dto.getUnitName()))
            throw new ConflictException("Unit '" + dto.getUnitName() + "' already exists");
        Unit unit = new Unit();
        unit.setUnitName(dto.getUnitName());
        unit.setUnitAbbreviation(dto.getUnitAbbreviation());
        return repository.save(unit);
    }

    @Transactional(readOnly = true)
    public Page<Unit> getAll(Pageable pageable) { return repository.findAll(pageable); }

    @Transactional(readOnly = true)
    public Unit getById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Unit", id));
    }

    public Unit update(UUID id, UnitRequestDto dto) {
        Unit unit = getById(id);
        if (repository.existsByUnitNameAndIdNot(dto.getUnitName(), id))
            throw new ConflictException("Unit '" + dto.getUnitName() + "' already exists");
        unit.setUnitName(dto.getUnitName());
        unit.setUnitAbbreviation(dto.getUnitAbbreviation());
        return repository.save(unit);
    }

    public void delete(UUID id) { repository.delete(getById(id)); }
}
