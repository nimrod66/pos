package com.example.pos.core.branch.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchRequestDto {

    @NotBlank(message = "Branch name is required")
    private String branchName;

    @NotBlank(message = "Branch code is required")
    @Size(min = 2, max = 20, message = "Branch code must be 2-20 characters")
    private String branchCode;

    @NotBlank(message = "Phone number is required")
    @Size(min = 10, max = 15, message = "Phone number must be 10-15 characters")
    private String phoneNumber;

    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Pharmacy ID is required")
    private UUID pharmacyId;

    private String status;
}

