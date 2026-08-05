package com.example.pos.masterdata.medicine.dto;

import com.example.pos.masterdata.medicine.model.Medicine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicineResponseDto {

    private UUID id;
    private String barcode;
    private String manufacturerBarcode;
    private String internalBarcode;
    private String barcodeType;
    private String barcodeSource;
    private String kemsaCode;
    private String ppbCode;
    private String etimsItemCode;
    private String gs1CompanyPrefix;
    private boolean trackSerialNumber;
    private boolean trackBatch;
    private boolean trackExpiry;
    private String sku;
    private String brandName;
    private String genericName;
    private String strength;
    private String status;
    private UUID manufacturerId;
    private String manufacturerName;
    private UUID medicineCategoriesId;
    private String categoryName;
    private UUID dosageFormId;
    private String dosageFormName;
    private UUID unitId;
    private String unitName;
    private UUID taxId;
    private String taxName;
    private boolean requiresPrescription;
    private String description;
    private Integer maximumDispenseQuantity;
    private String minimumAge;
    private boolean requiresRefrigeration;
    private boolean isControlledDrug;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MedicineResponseDto from(Medicine medicine) {
        return MedicineResponseDto.builder()
                .id(medicine.getId())
                .barcode(medicine.getBarcode())
                .manufacturerBarcode(medicine.getManufacturerBarcode())
                .internalBarcode(medicine.getInternalBarcode())
                .barcodeType(medicine.getBarcodeType() != null ? medicine.getBarcodeType().name() : null)
                .barcodeSource(medicine.getBarcodeSource() != null ? medicine.getBarcodeSource().name() : null)
                .kemsaCode(medicine.getKemsaCode())
                .ppbCode(medicine.getPpbCode())
                .etimsItemCode(medicine.getEtimsItemCode())
                .gs1CompanyPrefix(medicine.getGs1CompanyPrefix())
                .trackSerialNumber(medicine.isTrackSerialNumber())
                .trackBatch(medicine.isTrackBatch())
                .trackExpiry(medicine.isTrackExpiry())
                .sku(medicine.getSku())
                .brandName(medicine.getBrandName())
                .genericName(medicine.getGenericName())
                .strength(medicine.getStrength())
                .status(medicine.getStatus() != null ? medicine.getStatus().name() : null)
                .manufacturerId(medicine.getManufacturer() != null ? medicine.getManufacturer().getId() : null)
                .manufacturerName(medicine.getManufacturer() != null ? medicine.getManufacturer().getManufacturerName() : null)
                .medicineCategoriesId(medicine.getMedicineCategories() != null ? medicine.getMedicineCategories().getId() : null)
                .categoryName(medicine.getMedicineCategories() != null ? medicine.getMedicineCategories().getCategoryName() : null)
                .dosageFormId(medicine.getDosageForm() != null ? medicine.getDosageForm().getId() : null)
                .dosageFormName(medicine.getDosageForm() != null ? medicine.getDosageForm().getFormName() : null)
                .unitId(medicine.getUnit() != null ? medicine.getUnit().getId() : null)
                .unitName(medicine.getUnit() != null ? medicine.getUnit().getUnitName() : null)
                .taxId(medicine.getTax() != null ? medicine.getTax().getId() : null)
                .taxName(medicine.getTax() != null ? medicine.getTax().getTaxName() : null)
                .requiresPrescription(medicine.isRequiresPrescription())
                .description(medicine.getDescription())
                .maximumDispenseQuantity(medicine.getMaximumDispenseQuantity())
                .minimumAge(medicine.getMinimumAge())
                .requiresRefrigeration(medicine.isRequiresRefrigeration())
                .isControlledDrug(medicine.isControlledDrug())
                .createdAt(medicine.getCreatedAt())
                .updatedAt(medicine.getUpdatedAt())
                .build();
    }
}