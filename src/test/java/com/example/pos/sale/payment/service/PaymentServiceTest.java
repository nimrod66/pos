package com.example.pos.sale.payment.service;

import com.example.pos.sale.payment.model.Payment;
import com.example.pos.sale.payment.repository.PaymentRepository;
import com.example.pos.sale.sales.model.Sales;
import com.example.pos.sale.sales.repository.SalesRepository;
import com.example.pos.sale.sales.service.SaleService;
import com.example.pos.operations.service.OperationalMetricsService;
import com.example.pos.payment.gateway.PaymentGateway;
import com.example.pos.payment.gateway.PaymentGatewayResponse;
import com.example.pos.sync.config.TerminalConfig;
import com.example.pos.sync.service.SyncService;
import com.example.pos.security.auth.AuthenticatedUserContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

    @Mock
    private SaleService saleService;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private MpesaSettings mpesaSettings;

    @Mock
    private OperationalMetricsService metricsService;

    @Test
    void shouldKeepReservationWhenMpesaStatusIsTemporarilyUnavailable() {
        PaymentService paymentService = new PaymentService(paymentRepository, salesRepository, gatewayFactory,
                syncService, terminalConfig, current, saleService, mpesaSettings, metricsService);
        UUID branchId = UUID.randomUUID();
        UUID saleId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        Sales sale = Sales.builder().build();
        ReflectionTestUtils.setField(sale, "id", saleId);
        Payment payment = Payment.builder()
                .sales(sale)
                .paymentMethod(Payment.PaymentMethod.M_PESA)
                .amount(new BigDecimal("100.00"))
                .currency("KES")
                .paymentStatus("PROCESSING")
                .checkoutRequestId("ws_CO_pending")
                .build();
        ReflectionTestUtils.setField(payment, "id", paymentId);

        when(current.branchId()).thenReturn(branchId);
        when(paymentRepository.findByIdAndSalesBranchId(paymentId, branchId))
                .thenReturn(Optional.of(payment));
        when(gatewayFactory.getGateway(Payment.PaymentMethod.M_PESA)).thenReturn(paymentGateway);
        when(paymentGateway.queryStatus("ws_CO_pending")).thenReturn(
                PaymentGatewayResponse.builder()
                        .success(false)
                        .status(PaymentGatewayResponse.Status.PROCESSING.name())
                        .responseCode("QUERY_ERROR")
                        .responseDescription("Temporarily unavailable")
                        .build());

        PaymentGatewayResponse response = paymentService.queryStatus(paymentId);

        assert response.getStatus().equals("PROCESSING");
        assert payment.getPaymentStatus().equals("PROCESSING");
        verify(paymentRepository).save(payment);
        verify(saleService, never()).failOnlinePayment(any());
        verify(saleService, never()).finalizeOnlinePayment(any());
    }

    @Test
    void shouldProcessSuccessfulMpesaCallback() {
        PaymentService paymentService = new PaymentService(paymentRepository, salesRepository, gatewayFactory,
                syncService, terminalConfig, current, saleService, mpesaSettings, metricsService);
        when(terminalConfig.getTerminalId()).thenReturn("TERM-A");

        Sales sale = Sales.builder().build();
        sale.setTotal(new BigDecimal("100.00"));

        Payment payment = Payment.builder().build();
        payment.setAmount(new BigDecimal("100.00"));
        payment.setPaymentStatus("PENDING");
        payment.setSales(sale);
        payment.setTransactionReference("MREF-001");
        payment.setMerchantRequestId("MREF-001");

        when(paymentRepository.findByMerchantRequestId("MREF-001"))
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
        lenient().when(paymentRepository.saveAndFlush(any(Payment.class)))
                .thenAnswer(invocation -> paymentRepository.save(invocation.getArgument(0)));

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
        verify(saleService).finalizeOnlinePayment(sale.getId());
    }

    @Test
    void shouldHandleFailedMpesaCallback() {
        PaymentService paymentService = new PaymentService(paymentRepository, salesRepository, gatewayFactory,
                syncService, terminalConfig, current, saleService, mpesaSettings, metricsService);

        Sales sale = Sales.builder().build();
        sale.setTotal(new BigDecimal("50.00"));

        Payment payment = Payment.builder().build();
        payment.setAmount(new BigDecimal("50.00"));
        payment.setPaymentStatus("PENDING");
        payment.setSales(sale);
        payment.setTransactionReference("MREF-002");
        payment.setMerchantRequestId("MREF-002");

        when(paymentRepository.findByMerchantRequestId("MREF-002"))
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

        assert payment.getPaymentStatus().equals("CANCELLED");
        verify(saleService).failOnlinePayment(sale.getId());
    }

    @Test
    void shouldIgnoreAlreadyFinalizedPayment() {
        PaymentService paymentService = new PaymentService(paymentRepository, salesRepository, gatewayFactory,
                syncService, terminalConfig, current, saleService, mpesaSettings, metricsService);

        Payment payment = Payment.builder().build();
        payment.setPaymentStatus("COMPLETED");
        payment.setTransactionReference("MREF-003");
        payment.setMerchantRequestId("MREF-003");

        when(paymentRepository.findByMerchantRequestId("MREF-003"))
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
