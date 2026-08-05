package com.example.pos.masterdata.medicine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicineRequestDto {

    @NotBlank(message = "Barcode is required")
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

    private boolean trackBatch = true;

    private boolean trackExpiry = true;

    private String sku;

    @NotBlank(message = "Brand name is required")
    private String brandName;

    @NotBlank(message = "Generic name is required")
    private String genericName;

    private String strength;

    @NotNull(message = "Manufacturer ID is required")
    private UUID manufacturerId;

    @NotNull(message = "Category ID is required")
    private UUID medicineCategoriesId;

    private UUID dosageFormId;

    private UUID unitId;

    private UUID taxId;

    private boolean requiresPrescription;
    private String description;
    private Integer maximumDispenseQuantity;
    private String minimumAge;
    private boolean requiresRefrigeration;
    private boolean isControlledDrug;

    private String status;
}