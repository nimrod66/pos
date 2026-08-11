package com.example.pos.masterdata.medicine.repository;

import java.util.UUID;

import com.example.pos.masterdata.medicine.model.Medicine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MedicineRepository extends JpaRepository<Medicine, UUID> {

    String[] DETAILS = {"pharmacy", "manufacturer", "medicineCategories", "dosageForm", "unit", "tax"};

    @EntityGraph(attributePaths = {"pharmacy", "manufacturer", "medicineCategories", "dosageForm", "unit", "tax"})
    Optional<Medicine> findByIdAndPharmacyId(UUID id, UUID pharmacyId);

    @EntityGraph(attributePaths = {"pharmacy", "manufacturer", "medicineCategories", "dosageForm", "unit", "tax"})
    Page<Medicine> findByPharmacyId(UUID pharmacyId, Pageable pageable);

    @EntityGraph(attributePaths = {"pharmacy", "manufacturer", "medicineCategories", "dosageForm", "unit", "tax"})
    Optional<Medicine> findByPharmacyIdAndBarcode(UUID pharmacyId, String barcode);

    boolean existsByPharmacyIdAndBarcode(UUID pharmacyId, String barcode);

    boolean existsByPharmacyIdAndBarcodeAndIdNot(UUID pharmacyId, String barcode, UUID id);

    boolean existsByPharmacyIdAndSkuIgnoreCase(UUID pharmacyId, String sku);

    boolean existsByPharmacyIdAndSkuIgnoreCaseAndIdNot(UUID pharmacyId, String sku, UUID id);

    @EntityGraph(attributePaths = {"pharmacy", "manufacturer", "medicineCategories", "dosageForm", "unit", "tax"})
    Page<Medicine> findByPharmacyIdAndMedicineCategoriesId(UUID pharmacyId, UUID categoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"pharmacy", "manufacturer", "medicineCategories", "dosageForm", "unit", "tax"})
    Page<Medicine> findByPharmacyIdAndManufacturerId(UUID pharmacyId, UUID manufacturerId, Pageable pageable);

    @EntityGraph(attributePaths = {"pharmacy", "manufacturer", "medicineCategories", "dosageForm", "unit", "tax"})
    Page<Medicine> findByPharmacyIdAndIsControlledDrugTrue(UUID pharmacyId, Pageable pageable);

    @EntityGraph(attributePaths = {"pharmacy", "manufacturer", "medicineCategories", "dosageForm", "unit", "tax"})
    @Query("SELECT m FROM Medicine m WHERE m.pharmacy.id = :pharmacyId AND "
            + "(LOWER(m.brandName) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(m.genericName) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(m.barcode) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(m.sku) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Medicine> searchByPharmacy(@Param("pharmacyId") UUID pharmacyId,
                                    @Param("q") String q,
                                    Pageable pageable);

    @EntityGraph(attributePaths = {"pharmacy", "manufacturer", "medicineCategories", "dosageForm", "unit", "tax"})
    @Query("SELECT m FROM Medicine m WHERE m.pharmacy.id = :pharmacyId "
            + "AND m.status = com.example.pos.masterdata.medicine.model.Medicine.Status.AVAILABLE AND "
            + "(LOWER(m.brandName) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(m.genericName) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(m.barcode) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(m.sku) LIKE LOWER(CONCAT('%', :q, '%'))) "
            + "ORDER BY CASE WHEN LOWER(m.barcode) = LOWER(:q) THEN 0 ELSE 1 END, m.brandName")
    List<Medicine> searchForPos(@Param("pharmacyId") UUID pharmacyId,
                                @Param("q") String q,
                                Pageable pageable);

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
