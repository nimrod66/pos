package com.example.pos.procurement.goodsreceived.dto;

import com.example.pos.procurement.goodsreceived.model.GoodsReceivedNotes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceivedResponseDto {

    private Long id;
    private Long purchaseOrdersId;
    private Long userId;
    private String userName;
    private LocalDateTime receivedAt;
    private String remarks;
    private LocalDateTime createdAt;

    public static GoodsReceivedResponseDto from(GoodsReceivedNotes grn) {
        return GoodsReceivedResponseDto.builder()
                .id(grn.getId())
                .purchaseOrdersId(grn.getPurchaseOrders() != null ? grn.getPurchaseOrders().getId() : null)
                .userId(grn.getUser() != null ? grn.getUser().getId() : null)
                .userName(grn.getUser() != null ? grn.getUser().getFirstName() : null)
                .receivedAt(grn.getReceivedAt()).remarks(grn.getRemarks())
                .createdAt(grn.getCreatedAt()).build();
    }
}
