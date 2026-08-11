package com.example.pos.prescriptions.prescriptions.dto;

import com.example.pos.prescriptions.prescriptions.model.Prescriptions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionResponseDto {

    private UUID id;
    private UUID branchId;
    private UUID approvedById;
    private String customerName;
    private String doctorName;
    private String doctorLicenseNumber;
    private String hospitalName;
    private String prescriptionNumber;
    private String diagnosis;
    private LocalDate issuedDate;
    private String status;
    private LocalDateTime approvedAt;
    private LocalDateTime dispensedAt;
    private List<PrescriptionItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PrescriptionItemResponse {
        private UUID id;
        private UUID medicineId;
        private String medicineName;
        private String dosage;
        private Integer quantity;
    }

    public static PrescriptionResponseDto from(Prescriptions p) {
        return PrescriptionResponseDto.builder()
                .id(p.getId()).customerName(p.getCustomerName()).doctorName(p.getDoctorName())
                .branchId(p.getBranch() != null ? p.getBranch().getId() : null)
                .approvedById(p.getApprovedBy() != null ? p.getApprovedBy().getId() : null)
                .doctorLicenseNumber(p.getDoctorLicenseNumber()).hospitalName(p.getHospitalName())
                .prescriptionNumber(p.getPrescriptionNumber()).diagnosis(p.getDiagnosis())
                .issuedDate(p.getIssuedDate()).status(p.getStatus())
                .approvedAt(p.getApprovedAt()).dispensedAt(p.getDispensedAt())
                .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt()).build();
    }
}

