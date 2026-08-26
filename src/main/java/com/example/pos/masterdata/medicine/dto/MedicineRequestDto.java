package com.example.pos.masterdata.medicine.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicineRequestDto {

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

    @NotNull(message = "Buying price is required")
    @DecimalMin(value = "0.00")
    @Digits(integer = 12, fraction = 2)
    private BigDecimal buyingPrice;

    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.00")
    @Digits(integer = 12, fraction = 2)
    private BigDecimal sellingPrice;

    @NotNull(message = "Reorder level is required")
    @Min(0)
    private Integer reorderLevel;

    @NotNull(message = "Manufacturer ID is required")
    private UUID manufacturerId;

    @NotNull(message = "Category ID is required")
    private UUID medicineCategoriesId;

    private UUID dosageFormId;

    private UUID unitId;

    private UUID buyingUnitId;

    private Integer packSize;

    private UUID taxId;

    private boolean requiresPrescription;
    private String description;
    private Integer maximumDispenseQuantity;
    private String minimumAge;
    private boolean requiresRefrigeration;
    private boolean isControlledDrug;

    private String status;
}
