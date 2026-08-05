package com.example.pos.insurance.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.insurance.dto.InsuranceClaimRequestDto;
import com.example.pos.insurance.dto.InsuranceClaimResponseDto;
import com.example.pos.insurance.dto.InsurerRequestDto;
import com.example.pos.insurance.dto.InsurerResponseDto;
import com.example.pos.insurance.model.*;
import com.example.pos.insurance.service.InsuranceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/insurance")
public class InsuranceController {

    private final InsuranceService insuranceService;

    public InsuranceController(InsuranceService insuranceService) {
        this.insuranceService = insuranceService;
    }

    // ========== Insurers ==========

    @GetMapping("/insurers")
    public ResponseEntity<ApiResponse<PagedResponse<InsurerResponseDto>>> listInsurers(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String type) {
        Page<Insurer> page = insuranceService.listInsurers(type, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, InsurerResponseDto::from)));
    }

    @GetMapping("/insurers/active")
    public ResponseEntity<ApiResponse<List<InsurerResponseDto>>> listActiveInsurers() {
        var list = insuranceService.listActiveInsurers().stream().map(InsurerResponseDto::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/insurers/{id}")
    public ResponseEntity<ApiResponse<InsurerResponseDto>> getInsurer(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(InsurerResponseDto.from(insuranceService.getInsurer(id))));
    }

    @PostMapping("/insurers")
    public ResponseEntity<ApiResponse<InsurerResponseDto>> createInsurer(@Valid @RequestBody InsurerRequestDto dto) {
        var insurer = insuranceService.createInsurer(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(InsurerResponseDto.from(insurer)));
    }

    @PutMapping("/insurers/{id}")
    public ResponseEntity<ApiResponse<InsurerResponseDto>> updateInsurer(
            @PathVariable UUID id, @RequestBody InsurerRequestDto dto) {
        return ResponseEntity.ok(ApiResponse.updated(InsurerResponseDto.from(insuranceService.updateInsurer(id, dto))));
    }

    @DeleteMapping("/insurers/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInsurer(@PathVariable UUID id) {
        insuranceService.deleteInsurer(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    // ========== Schemes ==========

    @GetMapping("/schemes")
    public ResponseEntity<ApiResponse<PagedResponse<InsuranceScheme>>> listSchemes(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) UUID insurerId) {
        Page<InsuranceScheme> page = insuranceService.listSchemes(insurerId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.fromPage(page)));
    }

    @PostMapping("/insurers/{insurerId}/schemes")
    public ResponseEntity<ApiResponse<InsuranceScheme>> createScheme(
            @PathVariable UUID insurerId, @RequestBody InsuranceScheme scheme) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(insuranceService.createScheme(insurerId, scheme)));
    }

    // ========== Members ==========

    @GetMapping("/members")
    public ResponseEntity<ApiResponse<PagedResponse<InsuranceMember>>> listMembers(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) UUID insurerId) {
        Page<InsuranceMember> page = insuranceService.listMembers(insurerId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.fromPage(page)));
    }

    @GetMapping("/members/lookup")
    public ResponseEntity<ApiResponse<InsuranceMember>> lookupMember(
            @RequestParam String membershipNumber, @RequestParam UUID insurerId) {
        return ResponseEntity.ok(ApiResponse.ok(insuranceService.findMember(membershipNumber, insurerId)));
    }

    @PostMapping("/insurers/{insurerId}/members")
    public ResponseEntity<ApiResponse<InsuranceMember>> createMember(
            @PathVariable UUID insurerId, @RequestBody InsuranceMember member) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(insuranceService.createMember(insurerId, member)));
    }

    // ========== Authorizations ==========

    @GetMapping("/authorizations")
    public ResponseEntity<ApiResponse<PagedResponse<Authorization>>> listAuthorizations(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) UUID insurerId) {
        Page<Authorization> page = insuranceService.listAuthorizations(insurerId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.fromPage(page)));
    }

    @GetMapping("/authorizations/{id}")
    public ResponseEntity<ApiResponse<Authorization>> getAuthorization(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(insuranceService.getAuthorization(id)));
    }

    @PostMapping("/insurers/{insurerId}/authorizations")
    public ResponseEntity<ApiResponse<Authorization>> createAuthorization(
            @PathVariable UUID insurerId, @RequestBody Authorization auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(insuranceService.createAuthorization(insurerId, auth)));
    }

    // ========== Claims ==========

    @GetMapping("/claims")
    public ResponseEntity<ApiResponse<PagedResponse<InsuranceClaimResponseDto>>> listClaims(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) UUID insurerId,
            @RequestParam(required = false) String status) {
        Page<InsuranceClaim> page = insuranceService.listClaims(insurerId, status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, InsuranceClaimResponseDto::from)));
    }

    @GetMapping("/claims/{id}")
    public ResponseEntity<ApiResponse<InsuranceClaimResponseDto>> getClaim(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(InsuranceClaimResponseDto.from(insuranceService.getClaim(id))));
    }

    @GetMapping("/claims/by-sale/{saleId}")
    public ResponseEntity<ApiResponse<List<InsuranceClaimResponseDto>>> getClaimsBySale(@PathVariable UUID saleId) {
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
            @PathVariable UUID id, @RequestBody Map<String, Object> body) {
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
    public ResponseEntity<ApiResponse<PagedResponse<ClaimAttachment>>> listAttachments(
            @PageableDefault(size = 20) Pageable pageable,
            @PathVariable UUID claimId) {
        Page<ClaimAttachment> page = insuranceService.listAttachments(claimId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.fromPage(page)));
    }

    @PostMapping("/claims/{claimId}/attachments")
    public ResponseEntity<ApiResponse<ClaimAttachment>> addAttachment(
            @PathVariable UUID claimId, @RequestBody ClaimAttachment attachment) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(
                insuranceService.addAttachment(claimId, attachment)));
    }

    // ========== Batches ==========

    @GetMapping("/batches")
    public ResponseEntity<ApiResponse<PagedResponse<ClaimBatch>>> listBatches(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) UUID insurerId) {
        Page<ClaimBatch> page = insuranceService.listBatches(insurerId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.fromPage(page)));
    }

    @PostMapping("/insurers/{insurerId}/batches")
    public ResponseEntity<ApiResponse<ClaimBatch>> createBatch(@PathVariable UUID insurerId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(insuranceService.createBatch(insurerId)));
    }

    @PostMapping("/batches/{batchId}/submit")
    public ResponseEntity<ApiResponse<ClaimBatch>> submitBatch(@PathVariable UUID batchId) {
        return ResponseEntity.ok(ApiResponse.ok(insuranceService.submitBatch(batchId)));
    }

    // ========== Payments ==========

    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<PagedResponse<InsurancePayment>>> listPayments(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) UUID insurerId) {
        Page<InsurancePayment> page = insuranceService.listPayments(insurerId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.fromPage(page)));
    }

    @PostMapping("/insurers/{insurerId}/payments")
    public ResponseEntity<ApiResponse<InsurancePayment>> recordPayment(
            @PathVariable UUID insurerId, @RequestBody InsurancePayment payment) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(insuranceService.recordPayment(insurerId, payment)));
    }

    @PostMapping("/payments/{paymentId}/link")
    public ResponseEntity<ApiResponse<Void>> linkPaymentToClaims(
            @PathVariable UUID paymentId, @RequestBody Map<String, List<UUID>> body) {
        insuranceService.linkPaymentToClaims(paymentId, body.get("claimIds"));
        return ResponseEntity.ok(ApiResponse.ok(null, "Payment linked to claims"));
    }

    // ========== Reports ==========

    @GetMapping("/reports/{insurerId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInsurerReport(@PathVariable UUID insurerId) {
        return ResponseEntity.ok(ApiResponse.ok(insuranceService.generateInsurerReport(insurerId)));
    }

    // ========== Reconciliation ==========

    @GetMapping("/reconciliations")
    public ResponseEntity<ApiResponse<PagedResponse<ClaimReconciliation>>> listReconciliations(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) UUID insurerId) {
        Page<ClaimReconciliation> page = insuranceService.listReconciliations(insurerId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.fromPage(page)));
    }

    @PostMapping("/reconcile")
    public ResponseEntity<ApiResponse<ClaimReconciliation>> reconcile(
            @RequestBody Map<String, Object> body) {
        UUID insurerId = UUID.fromString(body.get("insurerId").toString());
        LocalDate start = LocalDate.parse((String) body.get("periodStart"));
        LocalDate end = LocalDate.parse((String) body.get("periodEnd"));
        return ResponseEntity.ok(ApiResponse.ok(insuranceService.runReconciliation(insurerId, start, end)));
    }
}
