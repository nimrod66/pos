package com.example.pos.insurance.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.insurance.adapter.InsuranceProviderAdapter;
import com.example.pos.insurance.dto.InsuranceClaimRequestDto;
import com.example.pos.insurance.dto.InsurerRequestDto;
import com.example.pos.insurance.model.*;
import com.example.pos.insurance.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
public class InsuranceService {

    private static final Logger log = LoggerFactory.getLogger(InsuranceService.class);

    private final InsurerRepository insurerRepository;
    private final InsuranceClaimRepository claimRepository;
    private final InsuranceSchemeRepository schemeRepository;
    private final InsuranceMemberRepository memberRepository;
    private final AuthorizationRepository authorizationRepository;
    private final ClaimBatchRepository batchRepository;
    private final ClaimAttachmentRepository attachmentRepository;
    private final InsurancePaymentRepository paymentRepository;
    private final ClaimReconciliationRepository reconciliationRepository;
    private final List<InsuranceProviderAdapter> providerAdapters;

    public InsuranceService(InsurerRepository insurerRepository,
                            InsuranceClaimRepository claimRepository,
                            InsuranceSchemeRepository schemeRepository,
                            InsuranceMemberRepository memberRepository,
                            AuthorizationRepository authorizationRepository,
                            ClaimBatchRepository batchRepository,
                            ClaimAttachmentRepository attachmentRepository,
                            InsurancePaymentRepository paymentRepository,
                            ClaimReconciliationRepository reconciliationRepository,
                            List<InsuranceProviderAdapter> providerAdapters) {
        this.insurerRepository = insurerRepository;
        this.claimRepository = claimRepository;
        this.schemeRepository = schemeRepository;
        this.memberRepository = memberRepository;
        this.authorizationRepository = authorizationRepository;
        this.batchRepository = batchRepository;
        this.attachmentRepository = attachmentRepository;
        this.paymentRepository = paymentRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.providerAdapters = providerAdapters;
    }

    // ========== Insurers ==========

    @Transactional(readOnly = true)
    public List<Insurer> listInsurers(String type) {
        if (type != null && !type.isBlank()) {
            return insurerRepository.findByInsurerType(Insurer.InsurerType.valueOf(type.toUpperCase()));
        }
        return insurerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Insurer> listActiveInsurers() {
        return insurerRepository.findByStatus(Insurer.Status.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Insurer getInsurer(Long id) {
        return insurerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Insurer", id));
    }

    public Insurer createInsurer(InsurerRequestDto dto) {
        if (insurerRepository.existsByCode(dto.getCode())) {
            throw new BadRequestException("Insurer code already exists: " + dto.getCode());
        }
        Insurer insurer = new Insurer();
        insurer.setName(dto.getName());
        insurer.setCode(dto.getCode().toUpperCase());
        insurer.setInsurerType(Insurer.InsurerType.valueOf(dto.getInsurerType().toUpperCase()));
        insurer.setContactPerson(dto.getContactPerson());
        insurer.setPhoneNumber(dto.getPhoneNumber());
        insurer.setEmail(dto.getEmail());
        insurer.setClaimSubmissionEmail(dto.getClaimSubmissionEmail());
        insurer.setPreauthPhone(dto.getPreauthPhone());
        insurer.setDefaultCoPayPercentage(dto.getDefaultCoPayPercentage());
        insurer.setDefaultCoPayFlat(dto.getDefaultCoPayFlat());
        insurer.setRequiresPreauth(dto.isRequiresPreauth());
        insurer.setMaxClaimAmount(dto.getMaxClaimAmount());
        if (dto.getStatus() != null) {
            insurer.setStatus(Insurer.Status.valueOf(dto.getStatus().toUpperCase()));
        }
        return insurerRepository.save(insurer);
    }

    public Insurer updateInsurer(Long id, InsurerRequestDto dto) {
        Insurer insurer = getInsurer(id);
        if (dto.getName() != null) insurer.setName(dto.getName());
        if (dto.getContactPerson() != null) insurer.setContactPerson(dto.getContactPerson());
        if (dto.getPhoneNumber() != null) insurer.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getEmail() != null) insurer.setEmail(dto.getEmail());
        if (dto.getClaimSubmissionEmail() != null) insurer.setClaimSubmissionEmail(dto.getClaimSubmissionEmail());
        if (dto.getPreauthPhone() != null) insurer.setPreauthPhone(dto.getPreauthPhone());
        if (dto.getDefaultCoPayPercentage() != null) insurer.setDefaultCoPayPercentage(dto.getDefaultCoPayPercentage());
        if (dto.getDefaultCoPayFlat() != null) insurer.setDefaultCoPayFlat(dto.getDefaultCoPayFlat());
        insurer.setRequiresPreauth(dto.isRequiresPreauth());
        if (dto.getMaxClaimAmount() != null) insurer.setMaxClaimAmount(dto.getMaxClaimAmount());
        if (dto.getStatus() != null) {
            insurer.setStatus(Insurer.Status.valueOf(dto.getStatus().toUpperCase()));
        }
        return insurerRepository.save(insurer);
    }

    public void deleteInsurer(Long id) {
        Insurer insurer = getInsurer(id);
        insurer.setStatus(Insurer.Status.INACTIVE);
        insurerRepository.save(insurer);
    }

    // ========== Schemes ==========

    @Transactional(readOnly = true)
    public List<InsuranceScheme> listSchemes(Long insurerId) {
        if (insurerId != null) return schemeRepository.findByInsurerId(insurerId);
        return schemeRepository.findAll();
    }

    public InsuranceScheme createScheme(Long insurerId, InsuranceScheme scheme) {
        Insurer insurer = getInsurer(insurerId);
        scheme.setInsurer(insurer);
        return schemeRepository.save(scheme);
    }

    // ========== Members ==========

    @Transactional(readOnly = true)
    public List<InsuranceMember> listMembers(Long insurerId) {
        if (insurerId != null) return memberRepository.findByInsurerId(insurerId);
        return memberRepository.findAll();
    }

    @Transactional(readOnly = true)
    public InsuranceMember findMember(String membershipNumber, Long insurerId) {
        return memberRepository.findByMembershipNumberAndInsurerId(membershipNumber, insurerId)
                .orElseThrow(() -> new ResourceNotFoundException("Member " + membershipNumber));
    }

    public InsuranceMember createMember(Long insurerId, InsuranceMember member) {
        Insurer insurer = getInsurer(insurerId);
        member.setInsurer(insurer);
        return memberRepository.save(member);
    }

    // ========== Authorizations ==========

    @Transactional(readOnly = true)
    public List<Authorization> listAuthorizations(Long insurerId) {
        if (insurerId != null) return authorizationRepository.findByInsurerId(insurerId);
        return authorizationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Authorization getAuthorization(Long id) {
        return authorizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Authorization", id));
    }

    public Authorization createAuthorization(Long insurerId, Authorization auth) {
        Insurer insurer = getInsurer(insurerId);
        auth.setInsurer(insurer);
        if (auth.getAuthorizationReference() == null) {
            auth.setAuthorizationReference("AUTH-" + insurer.getCode() + "-" +
                    UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        return authorizationRepository.save(auth);
    }

    // ========== Claims ==========

    public InsuranceClaim createClaim(InsuranceClaimRequestDto dto) {
        Insurer insurer = getInsurer(dto.getInsurerId());

        if (dto.getClaimAmount().compareTo(dto.getSaleTotal()) > 0) {
            throw new BadRequestException("Claim amount cannot exceed sale total");
        }

        InsuranceScheme scheme = dto.getSchemeId() != null
                ? schemeRepository.findById(dto.getSchemeId()).orElse(null) : null;

        InsuranceMember member = dto.getMemberId() != null
                ? memberRepository.findById(dto.getMemberId()).orElse(null) : null;

        Authorization authorization = dto.getAuthorizationId() != null
                ? getAuthorization(dto.getAuthorizationId()) : null;

        BigDecimal coPay = dto.getCoPayAmount() != null
                ? dto.getCoPayAmount()
                : calculateCoPay(insurer, scheme, dto.getClaimAmount(), dto.getSaleTotal());

        if (coPay.add(dto.getClaimAmount()).compareTo(dto.getSaleTotal()) > 0) {
            coPay = dto.getSaleTotal().subtract(dto.getClaimAmount());
        }

        if (dto.getApprovedAmount() != null && dto.getRejectedAmount() != null
                && dto.getApprovedAmount().add(dto.getRejectedAmount()).compareTo(dto.getClaimAmount()) != 0) {
            throw new BadRequestException("Approved + rejected must equal claim amount");
        }

        ClaimStatus initialStatus = ClaimStatus.PENDING;
        if (authorization != null && authorization.hasRemainingBalance()) {
            initialStatus = ClaimStatus.PREAUTH_OBTAINED;
        }

        InsuranceClaim claim = InsuranceClaim.builder()
                .insurer(insurer)
                .scheme(scheme)
                .member(member)
                .authorization(authorization)
                .saleId(dto.getSaleId())
                .patientName(dto.getPatientName() != null ? dto.getPatientName()
                        : (member != null ? member.getMemberName() : null))
                .patientMembershipId(dto.getPatientMembershipId() != null ? dto.getPatientMembershipId()
                        : (member != null ? member.getMembershipNumber() : null))
                .claimAmount(dto.getClaimAmount())
                .approvedAmount(dto.getApprovedAmount() != null ? dto.getApprovedAmount()
                        : dto.getClaimAmount())
                .rejectedAmount(dto.getRejectedAmount() != null ? dto.getRejectedAmount()
                        : BigDecimal.ZERO)
                .coPayAmount(coPay.setScale(2, RoundingMode.HALF_UP))
                .saleTotal(dto.getSaleTotal())
                .claimReference(generateClaimReference(insurer))
                .claimStatus(initialStatus)
                .notes(dto.getNotes())
                .build();

        if (authorization != null && authorization.hasRemainingBalance()) {
            authorization.setUsedAmount(authorization.getUsedAmount().add(claim.getApprovedAmount()));
            if (!authorization.hasRemainingBalance()) {
                authorization.setStatus(Authorization.AuthStatus.EXHAUSTED);
            }
            authorizationRepository.save(authorization);
        }

        claim = claimRepository.save(claim);
        log.info("Insurance claim created: {} for insurer {} (sale {})",
                claim.getClaimReference(), insurer.getName(), dto.getSaleId());
        return claim;
    }

    @Transactional(readOnly = true)
    public List<InsuranceClaim> listClaims(Long insurerId, String status) {
        if (insurerId != null && status != null) {
            return claimRepository.findByInsurerIdAndClaimStatus(insurerId, ClaimStatus.valueOf(status.toUpperCase()));
        }
        if (insurerId != null) return claimRepository.findByInsurerId(insurerId);
        if (status != null) return claimRepository.findByClaimStatus(ClaimStatus.valueOf(status.toUpperCase()));
        return claimRepository.findAll();
    }

    @Transactional(readOnly = true)
    public InsuranceClaim getClaim(Long id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("InsuranceClaim", id));
    }

    @Transactional(readOnly = true)
    public List<InsuranceClaim> getClaimsBySale(Long saleId) {
        return claimRepository.findBySaleId(saleId);
    }

    public InsuranceClaim updateClaimStatus(Long id, String status, String reason, BigDecimal approved, BigDecimal rejected) {
        InsuranceClaim claim = getClaim(id);
        ClaimStatus newStatus = ClaimStatus.valueOf(status.toUpperCase());
        claim.setClaimStatus(newStatus);
        if (newStatus == ClaimStatus.REJECTED && reason != null) claim.setRejectionReason(reason);
        if (approved != null) claim.setApprovedAmount(approved);
        if (rejected != null) claim.setRejectedAmount(rejected);
        if (newStatus == ClaimStatus.SUBMITTED) claim.setSubmittedAt(LocalDateTime.now());
        return claimRepository.save(claim);
    }

    // ========== Claim Attachments ==========

    @Transactional(readOnly = true)
    public List<ClaimAttachment> listAttachments(Long claimId) {
        return attachmentRepository.findByClaimId(claimId);
    }

    public ClaimAttachment addAttachment(Long claimId, ClaimAttachment attachment) {
        InsuranceClaim claim = getClaim(claimId);
        attachment.setClaim(claim);
        return attachmentRepository.save(attachment);
    }

    // ========== Batches ==========

    @Transactional(readOnly = true)
    public List<ClaimBatch> listBatches(Long insurerId) {
        if (insurerId != null) return batchRepository.findByInsurerId(insurerId);
        return batchRepository.findAll();
    }

    public ClaimBatch createBatch(Long insurerId) {
        Insurer insurer = getInsurer(insurerId);
        List<InsuranceClaim> pending = claimRepository.findByInsurerIdAndClaimStatus(
                insurerId, ClaimStatus.PENDING);

        String batchRef = "BTCH-" + insurer.getCode() + "-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));

        BigDecimal total = pending.stream()
                .map(InsuranceClaim::getApprovedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ClaimBatch batch = ClaimBatch.builder()
                .insurer(insurer)
                .batchReference(batchRef)
                .claimCount(pending.size())
                .totalAmount(total)
                .status(ClaimBatch.BatchStatus.DRAFT)
                .build();
        batch = batchRepository.save(batch);

        for (InsuranceClaim claim : pending) {
            claim.setBatch(batch);
            claim.setClaimStatus(ClaimStatus.SUBMITTED);
            claim.setSubmittedAt(LocalDateTime.now());
            claimRepository.save(claim);
        }

        log.info("Batch created: {} ({} claims, KSh {})", batchRef, pending.size(), total);
        return batch;
    }

    public ClaimBatch submitBatch(Long batchId) {
        ClaimBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("ClaimBatch", batchId));

        batch.setStatus(ClaimBatch.BatchStatus.SUBMITTED);
        batch.setSubmittedAt(LocalDateTime.now());
        ClaimBatch saved = batchRepository.save(batch);

        InsuranceProviderAdapter adapter = providerAdapters.stream()
                .filter(a -> a.getProvider().equalsIgnoreCase(
                        batch.getInsurer().getCode()) || a.getProvider().equals("FILE_EXPORT"))
                .filter(InsuranceProviderAdapter::isAvailable)
                .findFirst()
                .orElse(providerAdapters.stream()
                        .filter(a -> "FILE_EXPORT".equals(a.getProvider())).findFirst().orElse(null));

        if (adapter != null) {
            var result = adapter.submitBatch(saved);
            log.info("Batch {} submitted via {}: {}", saved.getBatchReference(), adapter.getProvider(), result.status());
        }

        return saved;
    }

    // ========== Payments ==========

    @Transactional(readOnly = true)
    public List<InsurancePayment> listPayments(Long insurerId) {
        if (insurerId != null) return paymentRepository.findByInsurerId(insurerId);
        return paymentRepository.findAll();
    }

    public InsurancePayment recordPayment(Long insurerId, InsurancePayment payment) {
        Insurer insurer = getInsurer(insurerId);
        payment.setInsurer(insurer);
        payment = paymentRepository.save(payment);
        log.info("Insurance payment recorded: {} for {} amount KSh {}",
                payment.getPaymentReference(), insurer.getName(), payment.getAmount());
        return payment;
    }

    public void linkPaymentToClaims(Long paymentId, List<Long> claimIds) {
        InsurancePayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("InsurancePayment", paymentId));

        BigDecimal totalLinked = BigDecimal.ZERO;
        for (Long claimId : claimIds) {
            InsuranceClaim claim = getClaim(claimId);
            claim.setPayment(payment);
            claim.setClaimStatus(ClaimStatus.PAID);
            claimRepository.save(claim);
            totalLinked = totalLinked.add(claim.getApprovedAmount());
        }
        log.info("Payment {} linked to {} claims (KSh {})", payment.getPaymentReference(), claimIds.size(), totalLinked);
    }

    // ========== Reconciliation ==========

    public ClaimReconciliation runReconciliation(Long insurerId, LocalDate start, LocalDate end) {
        Insurer insurer = getInsurer(insurerId);

        var existing = reconciliationRepository
                .findByInsurerIdAndPeriodStartAndPeriodEnd(insurerId, start, end);
        if (existing.isPresent()) return existing.get();

        List<InsuranceClaim> claims = claimRepository.findByInsurerId(insurerId);

        BigDecimal totalClaimed = claims.stream()
                .map(InsuranceClaim::getClaimAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalApproved = claims.stream()
                .map(InsuranceClaim::getApprovedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRejected = claims.stream()
                .map(InsuranceClaim::getRejectedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<InsurancePayment> payments = paymentRepository.findByInsurerId(insurerId);
        BigDecimal totalPaid = payments.stream()
                .map(InsurancePayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        int settledCount = (int) claims.stream()
                .filter(c -> c.getClaimStatus() == ClaimStatus.PAID
                        || c.getClaimStatus() == ClaimStatus.PARTIALLY_PAID).count();

        ClaimReconciliation rec = ClaimReconciliation.builder()
                .insurer(insurer)
                .periodStart(start).periodEnd(end)
                .totalClaimed(totalClaimed)
                .totalApproved(totalApproved)
                .totalRejected(totalRejected)
                .totalPaid(totalPaid)
                .outstanding(totalApproved.subtract(totalPaid))
                .claimCount(claims.size())
                .settledCount(settledCount)
                .build();

        return reconciliationRepository.save(rec);
    }

    @Transactional(readOnly = true)
    public List<ClaimReconciliation> listReconciliations(Long insurerId) {
        if (insurerId != null) return reconciliationRepository.findByInsurerId(insurerId);
        return reconciliationRepository.findAll();
    }

    // ========== Helpers ==========

    private BigDecimal calculateCoPay(Insurer insurer, InsuranceScheme scheme,
                                       BigDecimal claimAmount, BigDecimal saleTotal) {
        BigDecimal coPayPct = scheme != null && scheme.getCoPayPercentage() != null
                ? scheme.getCoPayPercentage()
                : insurer.getDefaultCoPayPercentage();
        BigDecimal coPayFlat = scheme != null && scheme.getCoPayFlat() != null
                ? scheme.getCoPayFlat()
                : insurer.getDefaultCoPayFlat();
        BigDecimal maxClaim = scheme != null && scheme.getMaxClaimAmount() != null
                ? scheme.getMaxClaimAmount()
                : insurer.getMaxClaimAmount();

        if (maxClaim != null && maxClaim.compareTo(BigDecimal.ZERO) > 0
                && claimAmount.compareTo(maxClaim) > 0) {
            return saleTotal.subtract(maxClaim);
        }

        BigDecimal percentageCoPay = BigDecimal.ZERO;
        if (coPayPct != null && coPayPct.compareTo(BigDecimal.ZERO) > 0) {
            percentageCoPay = saleTotal.multiply(
                    coPayPct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        }

        BigDecimal flatCoPay = coPayFlat != null ? coPayFlat : BigDecimal.ZERO;
        BigDecimal coPay = percentageCoPay.compareTo(flatCoPay) > 0 ? percentageCoPay : flatCoPay;

        if (coPay.add(claimAmount).compareTo(saleTotal) > 0) coPay = saleTotal.subtract(claimAmount);
        if (coPay.compareTo(BigDecimal.ZERO) < 0) coPay = BigDecimal.ZERO;
        return coPay.setScale(2, RoundingMode.HALF_UP);
    }

    private String generateClaimReference(Insurer insurer) {
        String prefix = insurer.getCode() != null ? insurer.getCode() : "INS";
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        return prefix + "-" + datePart + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
