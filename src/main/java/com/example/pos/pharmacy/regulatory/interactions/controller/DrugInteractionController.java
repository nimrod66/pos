package com.example.pos.pharmacy.regulatory.interactions.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.pharmacy.regulatory.interactions.dto.DrugInteractionResponse;
import com.example.pos.pharmacy.regulatory.interactions.model.DrugInteraction;
import com.example.pos.pharmacy.regulatory.interactions.service.DrugInteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/drug-interactions")
@RequiredArgsConstructor
public class DrugInteractionController {

    private final DrugInteractionService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DrugInteractionResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(service.findAll()));
    }

    @GetMapping("/check")
    public ResponseEntity<ApiResponse<List<DrugInteractionResponse>>> check(
            @RequestParam List<UUID> medicineIds) {
        return ResponseEntity.ok(ApiResponse.ok(service.checkInteractions(medicineIds)));
    }

    @GetMapping("/check/pair")
    public ResponseEntity<ApiResponse<List<DrugInteractionResponse>>> checkPair(
            @RequestParam UUID medicine1Id, @RequestParam UUID medicine2Id) {
        return ResponseEntity.ok(ApiResponse.ok(service.checkPair(medicine1Id, medicine2Id)));
    }
}
