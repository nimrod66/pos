package com.example.pos.procurement.suppliers.repository;

import com.example.pos.procurement.suppliers.model.Suppliers;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Suppliers, Long> {

    Optional<Suppliers> findBySupplierName(String name);
    boolean existsBySupplierName(String name);
    boolean existsBySupplierNameAndIdNot(String name, Long id);
}
