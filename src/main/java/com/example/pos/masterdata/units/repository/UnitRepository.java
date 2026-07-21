package com.example.pos.masterdata.units.repository;

import com.example.pos.masterdata.units.model.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UnitRepository extends JpaRepository<Unit, Long> {

    Optional<Unit> findByUnitName(String unitName);

    boolean existsByUnitName(String unitName);

    boolean existsByUnitNameAndIdNot(String unitName, Long id);
}
