package com.example.pos.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CatalogRepository extends JpaRepository<Catalog, Long> {

    List<Catalog> findBySupplier(String supplier);

    Optional<Catalog> findByNameAndCatalogVersion(String name, String catalogVersion);

    @Query("SELECT c FROM Catalog c WHERE c.status = 'ACTIVE' AND c.supplier = ?1")
    List<Catalog> findActiveBySupplier(String supplier);

    List<Catalog> findBySupplierAndStatus(String supplier, String status);
}
