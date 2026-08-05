package com.example.pos.masterdata.tax.repository;

import java.util.UUID;

import com.example.pos.masterdata.tax.model.Tax;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaxRepository extends JpaRepository<Tax, UUID> {

    Optional<Tax> findByTaxName(String taxName);

    Optional<Tax> findByCode(String code);

    boolean existsByTaxName(String taxName);

    boolean existsByCode(String code);

    boolean existsByTaxNameAndIdNot(String taxName, UUID id);

    boolean existsByCodeAndIdNot(String code, UUID id);

    List<Tax> findByActiveTrue();

    Page<Tax> findByActiveTrue(Pageable pageable);
}
