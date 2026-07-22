package com.example.pos.insurance.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.insurance.dto.InsuranceClaimRequestDto;
import com.example.pos.insurance.dto.InsuranceClaimResponseDto;
import com.example.pos.insurance.dto.InsurerRequestDto;
import com.example.pos.insurance.dto.InsurerResponseDto;
import com.example.pos.insurance.model.*;
import com.example.pos.insurance.service.InsuranceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/insurance")
public class InsuranceController {

    private final InsuranceService insuranceService;

    public InsuranceController(InsuranceService insuranceService) {
        this.insuranceService = insuranceService;
    }

    // ========== Insurers ==========

    @GetMapping("/insurers")
    public ResponseEntity<ApiResponse<List<InsurerResponseDto>>> listInsurers(
            @RequestParam(required = false) String type) {
        var list = insuranceService.listInsurers(type).stream().map(InsurerResponseDto::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/insurers/active")
    public ResponseEntity<ApiResponse<List<InsurerResponseDto>>> listActiveInsurers() {
        var list = insuranceService.listActiveInsurers().stream().map(InsurerResponseDto::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/insurers/{id}")
    public ResponseEntity<ApiResponse<InsurerResponseDto>> getInsurer(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(InsurerResponseDto.from(insuranceService.getInsurer(id))));
    }

    @PostMapping("/insurers")
    public ResponseEntity<ApiResponse<InsurerResponseDto>> createInsurer(@Valid @RequestBody InsurerRequestDto dto) {
        var insurer = insuranceService.createInsurer(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(InsurerResponseDto.from(insurer)));
    }

    @PutMapping("/insurers/{id}")
    public ResponseEntity<ApiResponse<InsurerResponseDto>> updateInsurer(
            @PathVariable Long id, @RequestBody InsurerRequestDto dto) {
        return ResponseEntity.ok(ApiResponse.updated(InsurerResponseDto.from(insuranceService.updateInsurer(id, dto))));
    }

    @DeleteMapping("/insurers/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInsurer(@PathVariable Long id) {
        insuranceService.deleteInsurer(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    // ========== Schemes ==========

    @GetMapping("/schemes")
    public ResponseEntity<ApiResponse<List<InsuranceScheme>>> listSchemes(
            @RequestParam(required = false) Long insurerId) {
        return ResponseEntity.ok(ApiResponse.ok(insuranceService.listSchemes(insurerId)));
    }

    @PostMapping("/insurers/{insurerId}/schemes")
    public ResponseEntity<ApiResponse<InsuranceScheme>> createScheme(
            @PathVariable Long insurerId, @RequestBody InsuranceScheme scheme) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(insuranceService.createScheme(insurerId, scheme)));
    }

    // ========== Members ==========

    @GetMapping("/members")
    public ResponseEntity<ApiResponse<List<InsuranceMember>>> listMembers(
            @RequestParam(required = false) Long insurerId) {
        return ResponseEntity.ok(ApiResponse.ok(insuranceService.listMembers(insurerId)));
    }

    @GetMapping("/members/lookup")
    public ResponseEntity<ApiResponse<InsuranceMember>> lookupMember(
            @RequestParam String membershipNumber, @RequestParam Long insurerId) {
        return ResponseEntity.ok(ApiResponse.ok(insuranceService.findMember(membershipNumber, insurerId)));
    }

    @PostMapping("/insurers/{insurerId}/members")
    public ResponseEntity<ApiResponse<InsuranceMember>> createMember(
            @PathVariable Long insurerId, @RequestBody InsuranceMember member) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(insuranceService.createMember(insurerId, member)));
    }

    // ========== Authorizations ==========

    @GetMapping("/authorizations")
    public ResponseEntity<ApiResponse<List<Authorization>>> listAuthorizations(
            @RequestParam(required = false) Long insurerId) {
        return ResponseEntity.ok(ApiResponse.ok(insuranceService.listAuthorizations(insurerId)));
    }

    @GetMapping("/authorizations/{id}")
    public ResponseEntity<ApiResponse<Authorization>> getAuthorization(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(insuranceService.getAuthorization(id)));
    }

    @PostMapping("/insurers/{insurerId}/authorizations")
    public ResponseEntity<ApiResponse<Authorization>> createAuthorization(
            @PathVariable Long insurerId, @RequestBody Authorization auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(insuranceService.createAuthorization(insurerId, auth)));
    }

    // ========== Claims ==========

    @GetMapping("/claims")
    public ResponseEntity<ApiResponse<List<InsuranceClaimResponseDto>>> listClaims(
            @RequestParam(required = false) Long insurerId,
            @RequestParam(required = false) String status) {
        var list = insuranceService.listClaims(insurerId, status).stream()
                .map(InsuranceClaimResponseDto::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/claims/{id}")
    public ResponseEntity<ApiResponse<InsuranceClaimResponseDto>> getClaim(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(InsuranceClaimResponseDto.from(insuranceService.getClaim(id))));
    }

    @GetMapping("/claims/by-sale/{saleId}")
    public ResponseEntity<ApiResponse<List<InsuranceClaimResponseDto>>> getClaimsBySale(@PathVariable Long saleId) {
        var list = insuranceService.getClaimsBySale(saleId).stream()
                .map(InsuranceClaimResponseDto::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/claims")
    public ResponseEntity<ApiResponse<InsuranceClaimResponseDto>> createClaim(
            @Valid @RequestBody InsuranceClaimRequestDto dto) {
        var claim = insuranceService.createClaim(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(InsuranceClaimResponseDto.from(claim)));
    }

    @PatchMapping("/claims/{id}/status")
    public ResponseEntity<ApiResponse<InsuranceClaimResponseDto>> updateClaimStatus(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        String status = (String) body.get("status");
        String reason = (String) body.get("reason");
        BigDecimal approved = body.get("approved") != null
                ? new BigDecimal(body.get("approved").toString()) : null;
        BigDecimal rejected = body.get("rejected") != null
                ? new BigDecimal(body.get("rejected").toString()) : null;
        var claim = insuranceService.updateClaimStatus(id, status, reason, approved, rejected);
        return ResponseEntity.ok(ApiResponse.ok(InsuranceClaimResponseDto.from(claim)));
    }

    // ========== Attachments ==========

    @GetMapping("/claims/{claimId}/attachments")
    public ResponseEntity<ApiResponse<List<ClaimAttachment>>> listAttachments(@PathVariable Long claimId) {
        return ResponseEntity.ok(ApiResponse.ok(insuranceService.listAttachments(claimId)));
    }

    @PostMapping("/claims/{claimId}/attachments")
    public ResponseEntity<ApiResponse<ClaimAttachment>> addAttachment(
            @PathVariable Long claimId, @RequestBody ClaimAttachment attachment) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(
                insuranceService.addAttachment(claimId, attachment)));
    }

    // ========== Batches ==========

    @GetMapping("/batches")
    public ResponseEntity<ApiResponse<List<ClaimBatch>>> listBatches(
            @RequestParam(required = false) Long insurerId) {
        return ResponseEntity.ok(ApiResponse.ok(insuranceService.listBatches(insurerId)));
    }

    @PostMapping("/insurers/{insurerId}/batches")
    public ResponseEntity<ApiResponse<ClaimBatch>> createBatch(@PathVariable Long insurerId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(insuranceService.createBatch(insurerId)));
    }

    @PostMapping("/batches/{batchId}/submit")
    public ResponseEntity<ApiResponse<ClaimBatch>> submitBatch(@PathVariable Long batchId) {
        return ResponseEntity.ok(ApiResponse.ok(insuranceService.submitBatch(batchId)));
    }

    // ========== Payments ==========

    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<List<InsurancePayment>>> listPayments(
            @RequestParam(required = false) Long insurerId) {
        return ResponseEntity.ok(ApiResponse.ok(insuranceService.listPayments(insurerId)));
    }

    @PostMapping("/insurers/{insurerId}/payments")
    public ResponseEntity<ApiResponse<InsurancePayment>> recordPayment(
            @PathVariable Long insurerId, @RequestBody InsurancePayment payment) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(insuranceService.recordPayment(insurerId, payment)));
    }

    @PostMapping("/payments/{paymentId}/link")
    public ResponseEntity<ApiResponse<Void>> linkPaymentToClaims(
            @PathVariable Long paymentId, @RequestBody Map<String, List<Long>> body) {
        insuranceService.linkPaymentToClaims(paymentId, body.get("claimIds"));
        return ResponseEntity.ok(ApiResponse.ok(null, "Payment linked to claims"));
    }

    // ========== Reconciliation ==========

    @GetMapping("/reconciliations")
    public ResponseEntity<ApiResponse<List<ClaimReconciliation>>> listReconciliations(
            @RequestParam(required = false) Long insurerId) {
        return ResponseEntity.ok(ApiResponse.ok(insuranceService.listReconciliations(insurerId)));
    }

    @PostMapping("/reconcile")
    public ResponseEntity<ApiResponse<ClaimReconciliation>> reconcile(
            @RequestBody Map<String, Object> body) {
        Long insurerId = Long.valueOf(body.get("insurerId").toString());
        LocalDate start = LocalDate.parse((String) body.get("periodStart"));
        LocalDate end = LocalDate.parse((String) body.get("periodEnd"));
        return ResponseEntity.ok(ApiResponse.ok(insuranceService.runReconciliation(insurerId, start, end)));
    }
}
