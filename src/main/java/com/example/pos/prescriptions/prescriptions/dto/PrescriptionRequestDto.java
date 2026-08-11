package com.example.pos.prescriptions.prescriptions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionRequestDto {

    @NotBlank private String customerName;
    @NotBlank private String doctorName;
    @NotBlank private String doctorLicenseNumber;
    private String hospitalName;
    @NotBlank private String prescriptionNumber;
    private String diagnosis;
    @NotNull private LocalDate issuedDate;

    @NotEmpty
    @Valid
    private List<PrescriptionItemDto> items;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PrescriptionItemDto {
        @NotNull private UUID medicineId;
        private String dosage;
        @NotNull @Positive private Integer quantity;
    }
}

