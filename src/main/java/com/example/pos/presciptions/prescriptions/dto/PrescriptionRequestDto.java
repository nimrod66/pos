package com.example.pos.presciptions.prescriptions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionRequestDto {

    @NotBlank private String customerName;
    private String doctorName;
    private String doctorLicenseNumber;
    private String hospitalName;
    private String prescriptionNumber;
    private String diagnosis;
    private LocalDate issuedDate;

    @NotNull
    private List<PrescriptionItemDto> items;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PrescriptionItemDto {
        @NotNull private Long medicineId;
        private String dosage;
        @NotNull private Integer quantity;
    }
}
