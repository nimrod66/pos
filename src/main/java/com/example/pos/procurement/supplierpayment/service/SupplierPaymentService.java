package com.example.pos.procurement.supplierpayment.service;

import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.procurement.supplierinvoices.model.SupplierInvoices;
import com.example.pos.procurement.supplierinvoices.repository.SupplierInvoicesRepository;
import com.example.pos.procurement.supplierpayment.dto.SupplierPaymentRequestDto;
import com.example.pos.procurement.supplierpayment.model.SupplierPayment;
import com.example.pos.procurement.supplierpayment.repository.SupplierPaymentRepository;
import com.example.pos.user.users.model.User;
import com.example.pos.user.users.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SupplierPaymentService {

    private final SupplierPaymentRepository repo;
    private final SupplierInvoicesRepository invoiceRepo;
    private final UserRepository userRepo;

    public SupplierPaymentService(SupplierPaymentRepository repo, SupplierInvoicesRepository invoiceRepo, UserRepository userRepo) {
        this.repo = repo;
        this.invoiceRepo = invoiceRepo;
        this.userRepo = userRepo;
    }

    public SupplierPayment makePayment(SupplierPaymentRequestDto dto) {
        SupplierInvoices invoice = invoiceRepo.findById(dto.getSupplierInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("SupplierInvoice", dto.getSupplierInvoiceId()));
        User user = userRepo.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", dto.getUserId()));

        SupplierPayment payment = new SupplierPayment();
        payment.setSupplierInvoices(invoice);
        payment.setUser(user);
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setPaymentAmount(dto.getPaymentAmount());
        payment.setPaymentReference(dto.getPaymentReference());
        payment.setPaymentDate(LocalDateTime.now());
        payment = repo.save(payment);

        BigDecimal totalPaid = repo.findBySupplierInvoicesId(invoice.getId()).stream()
                .map(SupplierPayment::getPaymentAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        invoice.setBalanceDue(invoice.getTotal().subtract(totalPaid));
        if (invoice.getBalanceDue().compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setStatus(SupplierInvoices.Status.PAID);
        }
        invoiceRepo.save(invoice);

        return payment;
    }

    @Transactional(readOnly = true)
    public Page<SupplierPayment> getByInvoice(UUID invoiceId, Pageable pageable) {
        List<SupplierPayment> list = repo.findBySupplierInvoicesId(invoiceId);
        return new PageImpl<>(list, pageable, list.size());
    }
}
