package com.example.pos.presciptions.dispensary.dto;

import com.example.pos.presciptions.dispensary.model.Dispensary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispensaryResponseDto {

    private Long id;
    private Long medicineBatchesId;
    private String batchNumber;
    private Long userId;
    private String userName;
    private Long prescriptionItemsId;
    private String medicineName;
    private Integer dispensedQuantity;
    private LocalDateTime dispensingDate;
    private LocalDateTime createdAt;

    public static DispensaryResponseDto from(Dispensary d) {
        return DispensaryResponseDto.builder()
                .id(d.getId())
                .medicineBatchesId(d.getMedicineBatches() != null ? d.getMedicineBatches().getId() : null)
                .batchNumber(d.getMedicineBatches() != null ? d.getMedicineBatches().getBatchNumber() : null)
                .userId(d.getUser() != null ? d.getUser().getId() : null)
                .userName(d.getUser() != null ? d.getUser().getFirstName() : null)
                .prescriptionItemsId(d.getPrescriptionItems() != null ? d.getPrescriptionItems().getId() : null)
                .medicineName(d.getPrescriptionItems() != null && d.getPrescriptionItems().getMedicine() != null
                        ? d.getPrescriptionItems().getMedicine().getBrandName() : null)
                .dispensedQuantity(d.getDispensedQuantity())
                .dispensingDate(d.getDispensingDate())
                .createdAt(d.getCreatedAt()).build();
    }
}
