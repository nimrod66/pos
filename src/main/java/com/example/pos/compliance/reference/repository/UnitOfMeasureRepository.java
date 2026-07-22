package com.example.pos.compliance.reference.repository;

import com.example.pos.compliance.reference.model.UnitOfMeasure;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UnitOfMeasureRepository extends JpaRepository<UnitOfMeasure, Long> {
    Optional<UnitOfMeasure> findByCode(String code);
}