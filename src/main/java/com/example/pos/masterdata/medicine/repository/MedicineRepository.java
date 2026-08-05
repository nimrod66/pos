package com.example.pos.masterdata.medicine.repository;

import java.util.UUID;

import com.example.pos.masterdata.medicine.model.Medicine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MedicineRepository extends JpaRepository<Medicine, UUID> {

    Optional<Medicine> findByBarcode(String barcode);

    Optional<Medicine> findBySku(String sku);

    boolean existsByBarcode(String barcode);

    boolean existsByBarcodeAndIdNot(String barcode, UUID id);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, UUID id);

    List<Medicine> findByMedicineCategoriesId(UUID categoryId);

    Page<Medicine> findByMedicineCategoriesId(UUID categoryId, Pageable pageable);

    List<Medicine> findByManufacturerId(UUID manufacturerId);

    Page<Medicine> findByManufacturerId(UUID manufacturerId, Pageable pageable);

    List<Medicine> findByIsControlledDrugTrue();

    Page<Medicine> findByIsControlledDrugTrue(Pageable pageable);

    List<Medicine> findByBrandNameContainingIgnoreCase(String brandName);

    List<Medicine> findByBarcodeContaining(String barcode);

    List<Medicine> findByBarcodeEndingWithOrderByBarcodeAsc(String partialBarcode);

    List<Medicine> findByBarcodeStartingWithOrderByBarcodeAsc(String partialBarcode);

    @Query("SELECT m FROM Medicine m WHERE LOWER(m.brandName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(m.genericName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(m.barcode) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Medicine> search(@Param("q") String q, Pageable pageable);
}
