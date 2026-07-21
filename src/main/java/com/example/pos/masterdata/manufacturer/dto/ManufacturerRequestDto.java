package com.example.pos.masterdata.manufacturer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManufacturerRequestDto {

    @NotBlank(message = "Manufacturer name is required")
    private String manufacturerName;

    private String manufacturerCountry;

    private String manufacturerContact;
}
