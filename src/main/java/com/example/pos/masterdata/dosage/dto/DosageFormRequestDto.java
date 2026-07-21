package com.example.pos.masterdata.dosage.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DosageFormRequestDto {

    @NotBlank(message = "Form name is required")
    private String formName;

    private String formDescription;
}
