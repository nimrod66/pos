package com.example.pos.procurement.suppliers.dto;

import com.example.pos.procurement.suppliers.model.Suppliers;
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
public class SupplierResponseDto {

    private UUID id;
    private UUID pharmacyId;
    private String supplierName;
    private String licenseNumber;
    private String phoneNumber;
    private String address;
    private String email;
    private String contactPerson;
    private String paymentTerms;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SupplierResponseDto from(Suppliers s) {
        return SupplierResponseDto.builder()
                .id(s.getId()).supplierName(s.getSupplierName()).licenseNumber(s.getLicenseNumber())
                .pharmacyId(s.getPharmacy() != null ? s.getPharmacy().getId() : null)
                .phoneNumber(s.getPhoneNumber()).address(s.getAddress()).email(s.getEmail())
                .contactPerson(s.getContactPerson()).paymentTerms(s.getPaymentTerms())
                .status(s.getStatus() != null ? s.getStatus().name() : null)
                .createdAt(s.getCreatedAt()).updatedAt(s.getUpdatedAt()).build();
    }
}

