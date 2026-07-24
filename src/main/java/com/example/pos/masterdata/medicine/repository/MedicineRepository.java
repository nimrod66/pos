package com.example.pos.masterdata.medicine.repository;

import com.example.pos.masterdata.medicine.model.Medicine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    Optional<Medicine> findByBarcode(String barcode);

    Optional<Medicine> findBySku(String sku);

    boolean existsByBarcode(String barcode);

    boolean existsByBarcodeAndIdNot(String barcode, Long id);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    List<Medicine> findByMedicineCategoriesId(Long categoryId);

    Page<Medicine> findByMedicineCategoriesId(Long categoryId, Pageable pageable);

    List<Medicine> findByManufacturerId(Long manufacturerId);

    Page<Medicine> findByManufacturerId(Long manufacturerId, Pageable pageable);

    List<Medicine> findByIsControlledDrugTrue();

    Page<Medicine> findByIsControlledDrugTrue(Pageable pageable);

    List<Medicine> findByBrandNameContainingIgnoreCase(String brandName);

    List<Medicine> findByBarcodeContaining(String barcode);

    List<Medicine> findByBarcodeEndingWithOrderByBarcodeAsc(String partialBarcode);

    List<Medicine> findByBarcodeStartingWithOrderByBarcodeAsc(String partialBarcode);

    @Query("SELECT m FROM Medicine m WHERE LOWER(m.brandName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(m.genericName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(m.barcode) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Medicine> search(@Param("q") String q, Pageable pageable);
}
