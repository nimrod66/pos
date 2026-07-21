package com.example.pos.masterdata.medicine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicineRequestDto {

    @NotBlank(message = "Barcode is required")
    private String barcode;

    private String sku;

    @NotBlank(message = "Brand name is required")
    private String brandName;

    @NotBlank(message = "Generic name is required")
    private String genericName;

    private String strength;

    @NotNull(message = "Manufacturer ID is required")
    private Long manufacturerId;

    @NotNull(message = "Category ID is required")
    private Long medicineCategoriesId;

    private Long dosageFormId;

    private Long unitId;

    private Long taxId;

    private boolean requiresPrescription;
    private String description;
    private Integer maximumDispenseQuantity;
    private String minimumAge;
    private boolean requiresRefrigeration;
    private boolean isControlledDrug;

    private String status;
}
