package com.example.pos.sale.payment.service;

import com.example.pos.sale.payment.model.Payment;
import com.example.pos.sale.payment.repository.PaymentRepository;
import com.example.pos.sale.sales.model.Sales;
import com.example.pos.sale.sales.repository.SalesRepository;
import com.example.pos.sync.config.TerminalConfig;
import com.example.pos.sync.service.SyncService;
import com.example.pos.security.auth.AuthenticatedUserContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SalesRepository salesRepository;

    @Mock
    private com.example.pos.payment.gateway.PaymentGatewayFactory gatewayFactory;

    @Mock
    private SyncService syncService;

    @Mock
    private TerminalConfig terminalConfig;

    @Mock
    private AuthenticatedUserContext current;

    @Test
    void shouldProcessSuccessfulMpesaCallback() {
        PaymentService paymentService = new PaymentService(paymentRepository, salesRepository, gatewayFactory,
                syncService, terminalConfig, current);
        when(terminalConfig.getTerminalId()).thenReturn("TERM-A");

        Sales sale = Sales.builder().build();
        sale.setTotal(new BigDecimal("100.00"));

        Payment payment = Payment.builder().build();
        payment.setAmount(new BigDecimal("100.00"));
        payment.setPaymentStatus("PENDING");
        payment.setSales(sale);
        payment.setTransactionReference("MREF-001");

        when(paymentRepository.findByTransactionReference("MREF-001"))
                .thenReturn(Optional.of(payment));
        lenient().when(paymentRepository.findBySalesId(any())).thenReturn(List.of(payment));
        lenient().when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) {
                try {
                    var idField = com.example.pos.common.BaseEntity.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(p, java.util.UUID.fromString(
                            "99999999-9999-9999-9999-999999999999"));
                } catch (Exception ignored) {}
            }
            return p;
        });

        Map<String, Object> callback = Map.of(
                "Body", Map.of(
                        "stkCallback", Map.of(
                                "ResultCode", "0",
                                "MerchantRequestID", "MREF-001",
                                "CheckoutRequestID", "ws_CO_123",
                                "ResultDesc", "Success"
                        )
                )
        );

        paymentService.handleMpesaCallback(callback);

        assert payment.getPaymentStatus().equals("COMPLETED");
        verify(syncService, times(1)).writeOutboxEvent(
                any(), eq("PAYMENT"), any(), any());
    }

    @Test
    void shouldHandleFailedMpesaCallback() {
        PaymentService paymentService = new PaymentService(paymentRepository, salesRepository, gatewayFactory,
                syncService, terminalConfig, current);

        Sales sale = Sales.builder().build();
        sale.setTotal(new BigDecimal("50.00"));

        Payment payment = Payment.builder().build();
        payment.setAmount(new BigDecimal("50.00"));
        payment.setPaymentStatus("PENDING");
        payment.setSales(sale);
        payment.setTransactionReference("MREF-002");

        when(paymentRepository.findByTransactionReference("MREF-002"))
                .thenReturn(Optional.of(payment));

        Map<String, Object> callback = Map.of(
                "Body", Map.of(
                        "stkCallback", Map.of(
                                "ResultCode", "1032",
                                "MerchantRequestID", "MREF-002",
                                "CheckoutRequestID", "ws_CO_456",
                                "ResultDesc", "Cancelled by user"
                        )
                )
        );

        paymentService.handleMpesaCallback(callback);

        assert payment.getPaymentStatus().equals("FAILED");
    }

    @Test
    void shouldIgnoreAlreadyFinalizedPayment() {
        PaymentService paymentService = new PaymentService(paymentRepository, salesRepository, gatewayFactory,
                syncService, terminalConfig, current);

        Payment payment = Payment.builder().build();
        payment.setPaymentStatus("COMPLETED");
        payment.setTransactionReference("MREF-003");

        when(paymentRepository.findByTransactionReference("MREF-003"))
                .thenReturn(Optional.of(payment));

        Map<String, Object> callback = Map.of(
                "Body", Map.of(
                        "stkCallback", Map.of(
                                "ResultCode", "0",
                                "MerchantRequestID", "MREF-003",
                                "CheckoutRequestID", "ws_CO_789",
                                "ResultDesc", "Success"
                        )
                )
        );

        paymentService.handleMpesaCallback(callback);

        verify(syncService, never()).writeOutboxEvent(any(), any(), any(), any());
    }
}
