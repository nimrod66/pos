package com.example.pos.masterdata.manufacturer.dto;

import com.example.pos.masterdata.manufacturer.model.Manufacturer;
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
public class ManufacturerResponseDto {

    private UUID id;
    private String manufacturerName;
    private String manufacturerCountry;
    private String manufacturerContact;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ManufacturerResponseDto from(Manufacturer manufacturer) {
        return ManufacturerResponseDto.builder()
                .id(manufacturer.getId())
                .manufacturerName(manufacturer.getManufacturerName())
                .manufacturerCountry(manufacturer.getManufacturerCountry())
                .manufacturerContact(manufacturer.getManufacturerContact())
                .createdAt(manufacturer.getCreatedAt())
                .updatedAt(manufacturer.getUpdatedAt())
                .build();
    }
}

