package com.example.pos.customer.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.customer.model.Customer;
import com.example.pos.customer.model.CustomerTransaction;
import com.example.pos.customer.repository.CustomerRepository;
import com.example.pos.customer.repository.CustomerTransactionRepository;
import com.example.pos.sale.payment.model.Payment;
import com.example.pos.sale.sales.model.Sales;
import com.example.pos.sale.sales.repository.SalesRepository;
import com.example.pos.security.auth.AuthenticatedUserContext;
import com.example.pos.user.users.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerAccountService {

    private final CustomerRepository customerRepository;
    private final CustomerTransactionRepository customerTransactionRepository;
    private final SalesRepository salesRepository;
    private final AuthenticatedUserContext current;

    public Customer getCustomer(UUID customerId) {
        User user = current.user();
        Branch branch = user.getBranch();
        return customerRepository.findByIdAndPharmacyId(customerId, branch.getPharmacy().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
    }

    public List<CustomerTransaction> getTransactionHistory(UUID customerId) {
        Customer customer = getCustomer(customerId);
        return customerTransactionRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId());
    }

    public BigDecimal getCurrentBalance(UUID customerId) {
        Customer customer = getCustomer(customerId);
        return customer.getBalance();
    }

    public Customer recordPayment(UUID customerId, BigDecimal amount, String description) {
        Customer customer = getCustomer(customerId);
        User user = current.user();

        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Payment amount must be positive",
                    "INVALID_PAYMENT_AMOUNT");
        }

        BigDecimal currentBalance = customer.getBalance();
        if (amount.compareTo(currentBalance) > 0) {
            throw new BadRequestException("Payment amount (" + amount
                    + ") exceeds outstanding balance (" + currentBalance + ")",
                    "PAYMENT_EXCEEDS_BALANCE");
        }

        BigDecimal newBalance = currentBalance.subtract(amount);
        customer.setBalance(newBalance);
        customerRepository.save(customer);

        customerTransactionRepository.save(CustomerTransaction.builder()
                .customer(customer)
                .transactionType(CustomerTransaction.TransactionType.PAYMENT.name())
                .amount(amount.negate())
                .runningBalance(newBalance)
                .description(description != null ? description : "Payment received")
                .recordedBy(user)
                .build());

        return customer;
    }

    public Customer adjustBalance(UUID customerId, BigDecimal amount, String reason) {
        Customer customer = getCustomer(customerId);
        User user = current.user();

        if (amount == null || amount.signum() == 0) {
            throw new BadRequestException("Adjustment amount cannot be zero",
                    "INVALID_ADJUSTMENT_AMOUNT");
        }

        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Adjustment reason is required",
                    "ADJUSTMENT_REASON_REQUIRED");
        }

        BigDecimal newBalance = customer.getBalance().add(amount);
        if (newBalance.signum() < 0) {
            throw new BadRequestException("Adjustment would result in negative balance",
                    "NEGATIVE_BALANCE_ADJUSTMENT");
        }

        customer.setBalance(newBalance);
        customerRepository.save(customer);

        customerTransactionRepository.save(CustomerTransaction.builder()
                .customer(customer)
                .transactionType(CustomerTransaction.TransactionType.ADJUSTMENT.name())
                .amount(amount)
                .runningBalance(newBalance)
                .description(reason)
                .recordedBy(user)
                .build());

        return customer;
    }

    public Customer updateCreditLimit(UUID customerId, BigDecimal creditLimit) {
        Customer customer = getCustomer(customerId);

        if (creditLimit != null && creditLimit.signum() < 0) {
            throw new BadRequestException("Credit limit cannot be negative",
                    "INVALID_CREDIT_LIMIT");
        }

        customer.setCreditLimit(creditLimit);
        return customerRepository.save(customer);
    }

    public List<Sales> getOutstandingSales(UUID customerId) {
        Customer customer = getCustomer(customerId);
        return salesRepository.findByCustomerAndAmountOwedGreaterThan(customer, BigDecimal.ZERO);
    }
}
