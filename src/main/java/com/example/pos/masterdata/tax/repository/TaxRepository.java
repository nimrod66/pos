package com.example.pos.masterdata.tax.repository;

import com.example.pos.masterdata.tax.model.Tax;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaxRepository extends JpaRepository<Tax, Long> {

    Optional<Tax> findByTaxName(String taxName);

    boolean existsByTaxName(String taxName);

    boolean existsByTaxNameAndIdNot(String taxName, Long id);
}
