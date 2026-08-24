package com.example.pos.sale.payment.repository;

import java.util.UUID;

import com.example.pos.sale.payment.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findBySalesId(UUID saleId);

    Page<Payment> findBySalesId(UUID saleId, Pageable pageable);

    @EntityGraph(attributePaths = {"sales", "sales.branch", "sales.user", "sales.customer"})
    Page<Payment> findBySalesIdAndSalesBranchId(UUID saleId, UUID branchId, Pageable pageable);

    @EntityGraph(attributePaths = {"sales", "sales.branch", "sales.user", "sales.customer"})
    Optional<Payment> findByIdAndSalesBranchId(UUID id, UUID branchId);

    Optional<Payment> findByTransactionReference(String transactionReference);

    Optional<Payment> findByMerchantRequestId(String merchantRequestId);

    Optional<Payment> findByCheckoutRequestId(String checkoutRequestId);

    boolean existsByTransactionReferenceIgnoreCase(String transactionReference);

    @Query("select coalesce(sum(payment.amount), 0) from Payment payment "
            + "where payment.sales.shift.id = :shiftId "
            + "and payment.paymentMethod = com.example.pos.sale.payment.model.Payment.PaymentMethod.CASH "
            + "and payment.paymentStatus = 'COMPLETED'")
    java.math.BigDecimal sumCompletedCashForShift(@Param("shiftId") UUID shiftId);

    @Query("select coalesce(sum(payment.amount), 0) from Payment payment "
            + "where payment.sales.shift.id = :shiftId "
            + "and payment.paymentMethod in ("
            + "com.example.pos.sale.payment.model.Payment.PaymentMethod.MPESA_MANUAL, "
            + "com.example.pos.sale.payment.model.Payment.PaymentMethod.M_PESA) "
            + "and payment.paymentStatus = 'COMPLETED'")
    java.math.BigDecimal sumCompletedMpesaForShift(@Param("shiftId") UUID shiftId);
}
