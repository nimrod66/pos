package com.example.pos.core.pharmacy.dto;

import com.example.pos.core.pharmacy.model.Pharmacy;
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
public class PharmacyResponseDto {

    private UUID id;
    private String name;
    private String address;
    private String email;
    private String phoneNumber;
    private String licenseNumber;
    private String kraPin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PharmacyResponseDto from(Pharmacy pharmacy) {
        return PharmacyResponseDto.builder()
                .id(pharmacy.getId())
                .name(pharmacy.getName())
                .address(pharmacy.getAddress())
                .email(pharmacy.getEmail())
                .phoneNumber(pharmacy.getPhoneNumber())
                .licenseNumber(pharmacy.getLicenseNumber())
                .kraPin(pharmacy.getKraPin())
                .createdAt(pharmacy.getCreatedAt())
                .updatedAt(pharmacy.getUpdatedAt())
                .build();
    }
}

