package com.example.pos.procurement.goodsreceived.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceivedRequestDto {

    @NotNull(message = "Purchase order ID is required")
    private Long purchaseOrdersId;

    @NotNull(message = "User ID is required")
    private Long userId;

    private String remarks;
}
