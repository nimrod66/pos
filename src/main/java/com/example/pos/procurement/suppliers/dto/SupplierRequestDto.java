package com.example.pos.procurement.suppliers.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierRequestDto {

    @NotBlank(message = "Supplier name is required")
    private String supplierName;

    private String licenseNumber;
    private String phoneNumber;
    private String address;
    private String email;
    private String contactPerson;
    private String paymentTerms;
    private String status;
}
