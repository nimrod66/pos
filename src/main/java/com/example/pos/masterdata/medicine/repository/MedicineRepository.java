package com.example.pos.masterdata.medicine.repository;

import com.example.pos.masterdata.medicine.model.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;

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

    List<Medicine> findByManufacturerId(Long manufacturerId);

    List<Medicine> findByIsControlledDrugTrue();

    List<Medicine> findByBrandNameContainingIgnoreCase(String brandName);

    List<Medicine> findByBarcodeContaining(String barcode);

    List<Medicine> findByBarcodeEndingWithOrderByBarcodeAsc(String partialBarcode);

    List<Medicine> findByBarcodeStartingWithOrderByBarcodeAsc(String partialBarcode);
}
