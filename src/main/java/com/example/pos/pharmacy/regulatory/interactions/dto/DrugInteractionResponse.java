package com.example.pos.pharmacy.regulatory.interactions.dto;

import com.example.pos.pharmacy.regulatory.interactions.model.DrugInteraction;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class DrugInteractionResponse {
    private UUID id;
    private UUID medicine1Id;
    private String medicine1Name;
    private UUID medicine2Id;
    private String medicine2Name;
    private DrugInteraction.Severity severity;
    private String description;

    public static DrugInteractionResponse from(DrugInteraction di) {
        return new DrugInteractionResponse(
            di.getId(),
            di.getMedicine1().getId(),
            di.getMedicine1().getBrandName(),
            di.getMedicine2().getId(),
            di.getMedicine2().getBrandName(),
            di.getSeverity(),
            di.getDescription()
        );
    }
}
