package com.example.pos.masterdata.medicine.service;

import com.example.pos.common.annotation.Auditable;
import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.masterdata.categories.model.MedicineCategories;
import com.example.pos.masterdata.categories.repository.MedicineCategoriesRepository;
import com.example.pos.masterdata.dosage.model.DosageForm;
import com.example.pos.masterdata.dosage.repository.DosageFormRepository;
import com.example.pos.masterdata.manufacturer.model.Manufacturer;
import com.example.pos.masterdata.manufacturer.repository.ManufacturerRepository;
import com.example.pos.masterdata.medicine.dto.MedicineRequestDto;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.masterdata.medicine.repository.MedicineRepository;
import com.example.pos.masterdata.tax.model.Tax;
import com.example.pos.masterdata.tax.repository.TaxRepository;
import com.example.pos.masterdata.units.model.Unit;
import com.example.pos.masterdata.units.repository.UnitRepository;
import com.example.pos.security.auth.AuthenticatedUserContext;
import com.example.pos.terminal.barcode.BarcodeSource;
import com.example.pos.terminal.barcode.BarcodeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.math.RoundingMode;
import java.util.Locale;

@Service
@Transactional
public class MedicineService {

    private final MedicineRepository medicineRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final MedicineCategoriesRepository categoriesRepository;
    private final DosageFormRepository dosageFormRepository;
    private final UnitRepository unitRepository;
    private final TaxRepository taxRepository;
    private final AuthenticatedUserContext current;

    public MedicineService(MedicineRepository medicineRepository,
                           ManufacturerRepository manufacturerRepository,
                           MedicineCategoriesRepository categoriesRepository,
                           DosageFormRepository dosageFormRepository,
                           UnitRepository unitRepository,
                           TaxRepository taxRepository,
                           AuthenticatedUserContext current) {
        this.medicineRepository = medicineRepository;
        this.manufacturerRepository = manufacturerRepository;
        this.categoriesRepository = categoriesRepository;
        this.dosageFormRepository = dosageFormRepository;
        this.unitRepository = unitRepository;
        this.taxRepository = taxRepository;
        this.current = current;
    }

    @Auditable(action = "CREATE_MEDICINE", entity = "Medicine")
    public Medicine createMedicine(MedicineRequestDto dto) {
        UUID pharmacyId = current.pharmacy().getId();
        String barcode = normalizeOptional(dto.getBarcode());
        String sku = normalizeSku(dto.getSku());
        if (barcode != null
                && medicineRepository.existsByPharmacyIdAndBarcode(pharmacyId, barcode)) {
            throw new ConflictException("Barcode " + dto.getBarcode() + " already exists");
        }
        if (sku != null && medicineRepository.existsByPharmacyIdAndSkuIgnoreCase(pharmacyId, sku)) {
            throw new ConflictException("SKU " + dto.getSku() + " already exists");
        }

        Medicine medicine = new Medicine();
        medicine.setPharmacy(current.pharmacy());
        resolveReferences(dto, medicine);
        mapToEntity(dto, medicine);
        medicine.setStatus(Medicine.Status.AVAILABLE);
        return medicineRepository.save(medicine);
    }

    @Transactional(readOnly = true)
    public Page<Medicine> getAllMedicines(Pageable pageable) {
        return medicineRepository.findByPharmacyId(current.pharmacy().getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Medicine> getMedicinesByCategory(UUID categoryId, Pageable pageable) {
        return medicineRepository.findByPharmacyIdAndMedicineCategoriesId(
                current.pharmacy().getId(), categoryId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Medicine> getMedicinesByManufacturer(UUID manufacturerId, Pageable pageable) {
        return medicineRepository.findByPharmacyIdAndManufacturerId(
                current.pharmacy().getId(), manufacturerId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Medicine> getControlledDrugs(Pageable pageable) {
        return medicineRepository.findByPharmacyIdAndIsControlledDrugTrue(
                current.pharmacy().getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Medicine> search(String q, Pageable pageable) {
        return medicineRepository.searchByPharmacy(current.pharmacy().getId(), q.trim(), pageable);
    }

    @Transactional(readOnly = true)
    public Medicine getMedicineById(UUID id) {
        return medicineRepository.findByIdAndPharmacyId(id, current.pharmacy().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Medicine", id));
    }

    @Transactional(readOnly = true)
    public Medicine getMedicineByBarcode(String barcode) {
        return medicineRepository.findByPharmacyIdAndBarcode(current.pharmacy().getId(), barcode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Medicine with barcode " + barcode));
    }

    @Auditable(action = "UPDATE_MEDICINE", entity = "Medicine")
    public Medicine updateMedicine(UUID id, MedicineRequestDto dto) {
        Medicine medicine = getMedicineById(id);
        UUID pharmacyId = current.pharmacy().getId();
        String barcode = normalizeOptional(dto.getBarcode());
        String sku = normalizeSku(dto.getSku());

        if (barcode != null
                && medicineRepository.existsByPharmacyIdAndBarcodeAndIdNot(pharmacyId, barcode, id)) {
            throw new ConflictException("Barcode " + dto.getBarcode() + " already exists");
        }
        if (sku != null && medicineRepository.existsByPharmacyIdAndSkuIgnoreCaseAndIdNot(
                pharmacyId, sku, id)) {
            throw new ConflictException("SKU " + dto.getSku() + " already exists");
        }

        resolveReferences(dto, medicine);
        mapToEntity(dto, medicine);
        return medicineRepository.save(medicine);
    }

    @Auditable(action = "DELETE_MEDICINE", entity = "Medicine")
    public void deleteMedicine(UUID id) {
        Medicine medicine = getMedicineById(id);
        medicine.setStatus(Medicine.Status.NOT_AVAILABLE);
        medicineRepository.save(medicine);
    }

    private void resolveReferences(MedicineRequestDto dto, Medicine medicine) {
        Manufacturer manufacturer = manufacturerRepository.findById(dto.getManufacturerId())
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer", dto.getManufacturerId()));
        medicine.setManufacturer(manufacturer);

        MedicineCategories category = categoriesRepository.findById(dto.getMedicineCategoriesId())
                .orElseThrow(() -> new ResourceNotFoundException("MedicineCategories", dto.getMedicineCategoriesId()));
        medicine.setMedicineCategories(category);

        if (dto.getDosageFormId() != null) {
            DosageForm form = dosageFormRepository.findById(dto.getDosageFormId())
                    .orElseThrow(() -> new ResourceNotFoundException("DosageForm", dto.getDosageFormId()));
            medicine.setDosageForm(form);
        }

        if (dto.getUnitId() != null) {
            Unit unit = unitRepository.findById(dto.getUnitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Unit", dto.getUnitId()));
            medicine.setUnit(unit);
        }

        if (dto.getTaxId() != null) {
            Tax tax = taxRepository.findById(dto.getTaxId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tax", dto.getTaxId()));
            medicine.setTax(tax);
        }
    }

    private void mapToEntity(MedicineRequestDto dto, Medicine medicine) {
        medicine.setBarcode(normalizeOptional(dto.getBarcode()));
        medicine.setManufacturerBarcode(dto.getManufacturerBarcode());
        medicine.setInternalBarcode(dto.getInternalBarcode());
        medicine.setKemsaCode(dto.getKemsaCode());
        medicine.setPpbCode(dto.getPpbCode());
        medicine.setEtimsItemCode(dto.getEtimsItemCode());
        medicine.setGs1CompanyPrefix(dto.getGs1CompanyPrefix());
        if (dto.getBarcodeType() != null) {
            try {
                medicine.setBarcodeType(BarcodeType.valueOf(dto.getBarcodeType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid barcode type: " + dto.getBarcodeType());
            }
        }
        if (dto.getBarcodeSource() != null) {
            try {
                medicine.setBarcodeSource(BarcodeSource.valueOf(dto.getBarcodeSource().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid barcode source: " + dto.getBarcodeSource());
            }
        }
        medicine.setTrackSerialNumber(dto.isTrackSerialNumber());
        medicine.setTrackBatch(dto.isTrackBatch());
        medicine.setTrackExpiry(dto.isTrackExpiry());
        medicine.setSku(normalizeSku(dto.getSku()));
        medicine.setBrandName(dto.getBrandName().trim());
        medicine.setGenericName(dto.getGenericName().trim());
        medicine.setStrength(dto.getStrength());
        medicine.setBuyingPrice(money(dto.getBuyingPrice()));
        medicine.setSellingPrice(money(dto.getSellingPrice()));
        medicine.setReorderLevel(dto.getReorderLevel());
        medicine.setRequiresPrescription(dto.isRequiresPrescription());
        medicine.setDescription(dto.getDescription());
        medicine.setMaximumDispenseQuantity(dto.getMaximumDispenseQuantity());
        medicine.setMinimumAge(dto.getMinimumAge());
        medicine.setRequiresRefrigeration(dto.isRequiresRefrigeration());
        medicine.setControlledDrug(dto.isControlledDrug());
        if (dto.getStatus() != null) {
            try {
                medicine.setStatus(Medicine.Status.valueOf(dto.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status: " + dto.getStatus());
            }
        }
    }

    private String normalizeSku(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private java.math.BigDecimal money(java.math.BigDecimal value) {
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new BadRequestException("Money values may have at most two decimal places",
                    "INVALID_MONEY_SCALE");
        }
    }
}
