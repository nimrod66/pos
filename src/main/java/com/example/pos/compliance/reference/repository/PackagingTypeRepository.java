package com.example.pos.compliance.reference.repository;

import com.example.pos.compliance.reference.model.PackagingType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PackagingTypeRepository extends JpaRepository<PackagingType, Long> {
    Optional<PackagingType> findByCode(String code);
}