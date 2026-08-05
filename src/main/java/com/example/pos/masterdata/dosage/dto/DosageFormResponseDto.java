package com.example.pos.masterdata.dosage.dto;

import com.example.pos.masterdata.dosage.model.DosageForm;
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
public class DosageFormResponseDto {

    private UUID id;
    private String formName;
    private String formDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DosageFormResponseDto from(DosageForm form) {
        return DosageFormResponseDto.builder()
                .id(form.getId())
                .formName(form.getFormName())
                .formDescription(form.getFormDescription())
                .createdAt(form.getCreatedAt())
                .updatedAt(form.getUpdatedAt())
                .build();
    }
}

