package com.example.pos.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, Long> {

    List<CatalogItem> findByCatalogId(Long catalogId);

    Optional<CatalogItem> findBySupplierCodeAndCatalogId(String supplierCode, Long catalogId);

    List<CatalogItem> findByMatchedMedicineIdIsNullAndCatalogId(Long catalogId);

    List<CatalogItem> findByMatchedMedicineId(Long medicineId);

    @Query("SELECT ci FROM CatalogItem ci WHERE ci.catalog.status = 'ACTIVE' AND ci.matchedMedicineId IS NULL")
    List<CatalogItem> findUnmatchedActiveItems();

    @Query("SELECT ci FROM CatalogItem ci WHERE ci.catalog.status = 'ACTIVE' AND ci.supplierCode = ?1")
    Optional<CatalogItem> findBySupplierCodeActive(String supplierCode);
}
