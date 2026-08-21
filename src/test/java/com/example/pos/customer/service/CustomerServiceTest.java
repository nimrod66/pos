package com.example.pos.customer.service;

import com.example.pos.common.exception.ConflictException;
import com.example.pos.core.pharmacy.model.Pharmacy;
import com.example.pos.customer.model.Customer;
import com.example.pos.customer.repository.CustomerRepository;
import com.example.pos.sale.sales.repository.SalesRepository;
import com.example.pos.security.auth.AuthenticatedUserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    private static final UUID CUSTOMER_ID =
            UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID PHARMACY_ID =
            UUID.fromString("20000000-0000-4000-8000-000000000001");

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private SalesRepository salesRepository;
    @Mock
    private AuthenticatedUserContext current;
    @Mock
    private Pharmacy pharmacy;
    @Mock
    private Customer customer;

    private CustomerService service;

    @BeforeEach
    void setUp() {
        service = new CustomerService(customerRepository, salesRepository, current);
        when(current.pharmacy()).thenReturn(pharmacy);
        when(pharmacy.getId()).thenReturn(PHARMACY_ID);
        when(customerRepository.findByIdAndPharmacyId(CUSTOMER_ID, PHARMACY_ID))
                .thenReturn(Optional.of(customer));
    }

    @Test
    void deletesCustomerWithoutSales() {
        when(salesRepository.existsByCustomerId(CUSTOMER_ID)).thenReturn(false);

        service.delete(CUSTOMER_ID);

        verify(customerRepository).delete(customer);
    }

    @Test
    void retainsCustomerLinkedToReceiptHistory() {
        when(salesRepository.existsByCustomerId(CUSTOMER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(CUSTOMER_ID))
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode")
                .isEqualTo("CUSTOMER_HAS_SALES");
        verify(customerRepository, never()).delete(customer);
    }
}
