package com.example.pos.procurement.suppliers.repository;

import com.example.pos.procurement.suppliers.model.Suppliers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Suppliers, Long> {

    Optional<Suppliers> findBySupplierName(String name);
    boolean existsBySupplierName(String name);
    boolean existsBySupplierNameAndIdNot(String name, Long id);

    @Query("SELECT s FROM Suppliers s WHERE LOWER(s.supplierName) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Suppliers> search(@Param("q") String q, Pageable pageable);
}
