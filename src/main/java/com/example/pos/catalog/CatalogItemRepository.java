package com.example.pos.catalog;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, UUID> {

    List<CatalogItem> findByCatalogId(UUID catalogId);

    Optional<CatalogItem> findBySupplierCodeAndCatalogId(String supplierCode, UUID catalogId);

    List<CatalogItem> findByMatchedMedicineIdIsNullAndCatalogId(UUID catalogId);

    List<CatalogItem> findByMatchedMedicineId(UUID medicineId);

    @Query("SELECT ci FROM CatalogItem ci WHERE ci.catalog.status = 'ACTIVE' AND ci.matchedMedicineId IS NULL")
    List<CatalogItem> findUnmatchedActiveItems();

    @Query("SELECT ci FROM CatalogItem ci WHERE ci.catalog.status = 'ACTIVE' AND ci.supplierCode = ?1")
    Optional<CatalogItem> findBySupplierCodeActive(String supplierCode);
}
