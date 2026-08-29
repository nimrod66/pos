package com.example.pos.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequestDto {

    @NotBlank private String firstName;
    private String lastName;
    private String phoneNumber;
    private String email;
    private String address;
    private String notes;
    private String kraPin;
    private LocalDate dateOfBirth;
    private String bloodType;
    private String allergies;
    private String medicalHistory;
}
