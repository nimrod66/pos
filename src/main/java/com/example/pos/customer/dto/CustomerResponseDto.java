package com.example.pos.customer.dto;

import com.example.pos.customer.model.Customer;
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
public class CustomerResponseDto {

    private UUID id;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String email;
    private String address;
    private Integer loyaltyPoints;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CustomerResponseDto from(Customer c) {
        return CustomerResponseDto.builder()
                .id(c.getId()).firstName(c.getFirstName()).lastName(c.getLastName())
                .phoneNumber(c.getPhoneNumber()).email(c.getEmail()).address(c.getAddress())
                .loyaltyPoints(c.getLoyaltyPoints()).notes(c.getNotes())
                .createdAt(c.getCreatedAt()).updatedAt(c.getUpdatedAt()).build();
    }
}

