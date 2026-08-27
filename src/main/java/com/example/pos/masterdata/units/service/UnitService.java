package com.example.pos.masterdata.units.service;

import com.example.pos.common.exception.BadRequestException;
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
        unit.setConversionFactor(dto.getConversionFactor() != null ? dto.getConversionFactor() : 1);
        if (dto.getParentUnitId() != null) {
            Unit parent = repository.findById(dto.getParentUnitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent unit", dto.getParentUnitId()));
            if (parent.getId().equals(unit.getId())) {
                throw new BadRequestException("A unit cannot be its own parent", "SELF_REFERENTIAL_UNIT");
            }
            unit.setParentUnit(parent);
        }
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
        unit.setConversionFactor(dto.getConversionFactor() != null ? dto.getConversionFactor() : 1);
        if (dto.getParentUnitId() != null) {
            Unit parent = repository.findById(dto.getParentUnitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent unit", dto.getParentUnitId()));
            if (parent.getId().equals(id)) {
                throw new BadRequestException("A unit cannot be its own parent", "SELF_REFERENTIAL_UNIT");
            }
            unit.setParentUnit(parent);
        } else {
            unit.setParentUnit(null);
        }
        return repository.save(unit);
    }

    public void delete(UUID id) { repository.delete(getById(id)); }

    @Transactional(readOnly = true)
    public int convertToBase(int quantity, Unit unit) {
        if (unit == null) return quantity;
        int total = quantity;
        Unit current = unit;
        while (current.getParentUnit() != null) {
            total *= current.getConversionFactor() != null ? current.getConversionFactor() : 1;
            current = current.getParentUnit();
        }
        return total;
    }
}
