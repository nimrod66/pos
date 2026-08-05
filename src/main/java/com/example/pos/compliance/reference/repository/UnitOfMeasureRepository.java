package com.example.pos.compliance.reference.repository;

import java.util.UUID;

import com.example.pos.compliance.reference.model.UnitOfMeasure;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UnitOfMeasureRepository extends JpaRepository<UnitOfMeasure, UUID> {
    Optional<UnitOfMeasure> findByCode(String code);
}