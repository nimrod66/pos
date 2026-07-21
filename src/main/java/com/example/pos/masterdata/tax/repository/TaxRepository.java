package com.example.pos.masterdata.tax.repository;

import com.example.pos.masterdata.tax.model.Tax;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaxRepository extends JpaRepository<Tax, Long> {

    Optional<Tax> findByTaxName(String taxName);

    Optional<Tax> findByCode(String code);

    boolean existsByTaxName(String taxName);

    boolean existsByCode(String code);

    boolean existsByTaxNameAndIdNot(String taxName, Long id);

    boolean existsByCodeAndIdNot(String code, Long id);

    List<Tax> findByActiveTrue();
}
