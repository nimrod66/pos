package com.example.pos.customer.service;

import com.example.pos.common.annotation.Auditable;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.BadRequestException;
import com.example.pos.customer.dto.CustomerRequestDto;
import com.example.pos.customer.model.Customer;
import com.example.pos.customer.repository.CustomerRepository;
import com.example.pos.sale.sales.repository.SalesRepository;
import com.example.pos.security.auth.AuthenticatedUserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Locale;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository repo;
    private final SalesRepository salesRepository;
    private final AuthenticatedUserContext current;

    public CustomerService(CustomerRepository repo,
                           SalesRepository salesRepository,
                           AuthenticatedUserContext current) {
        this.repo = repo;
        this.salesRepository = salesRepository;
        this.current = current;
    }

    @Auditable(action = "CREATE_CUSTOMER", entity = "Customer")
    public Customer create(CustomerRequestDto dto) {
        validateUnique(dto, null);
        Customer c = Customer.builder()
                .pharmacy(current.pharmacy())
                .firstName(dto.getFirstName().trim()).lastName(trimToNull(dto.getLastName()))
                .phoneNumber(trimToNull(dto.getPhoneNumber())).email(normalizeEmail(dto.getEmail()))
                .kraPin(trimToNull(dto.getKraPin()))
                .address(dto.getAddress()).notes(dto.getNotes()).build();
        return repo.save(c);
    }

    @Transactional(readOnly = true)
    public Page<Customer> getAll(Pageable pageable) {
        return repo.findByPharmacyId(current.pharmacy().getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Customer> search(String q, Pageable pageable) {
        return repo.searchByPharmacy(current.pharmacy().getId(), q.trim(), pageable);
    }

    @Transactional(readOnly = true)
    public Customer getById(UUID id) {
        return repo.findByIdAndPharmacyId(id, current.pharmacy().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    @Transactional(readOnly = true)
    public Customer findByPhone(String phone) {
        return repo.findByPharmacyIdAndPhoneNumber(current.pharmacy().getId(), phone.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Customer with phone " + phone));
    }

    @Auditable(action = "UPDATE_CUSTOMER", entity = "Customer")
    public Customer update(UUID id, CustomerRequestDto dto) {
        Customer c = getById(id);
        validateUnique(dto, id);
        c.setFirstName(dto.getFirstName().trim()); c.setLastName(trimToNull(dto.getLastName()));
        c.setPhoneNumber(trimToNull(dto.getPhoneNumber())); c.setKraPin(trimToNull(dto.getKraPin()));
        c.setEmail(normalizeEmail(dto.getEmail()));
        c.setAddress(dto.getAddress()); c.setNotes(dto.getNotes());
        return repo.save(c);
    }

    @Auditable(action = "DELETE_CUSTOMER", entity = "Customer")
    public void delete(UUID id) {
        Customer customer = getById(id);
        if (salesRepository.existsByCustomerId(id)) {
            throw new ConflictException(
                    "Customers linked to sales cannot be deleted because receipt history must be retained",
                    "CUSTOMER_HAS_SALES");
        }
        repo.delete(customer);
    }

    public Customer addLoyaltyPoints(UUID id, int points) {
        if (points == 0) throw new BadRequestException("Loyalty adjustment cannot be zero");
        Customer c = getById(id);
        int updated = c.getLoyaltyPoints() + points;
        if (updated < 0) throw new BadRequestException("Loyalty points cannot be negative");
        c.setLoyaltyPoints(updated);
        return repo.save(c);
    }

    private void validateUnique(CustomerRequestDto dto, UUID existingId) {
        UUID pharmacyId = current.pharmacy().getId();
        String phone = trimToNull(dto.getPhoneNumber());
        String email = normalizeEmail(dto.getEmail());
        boolean phoneExists = phone != null && (existingId == null
                ? repo.existsByPharmacyIdAndPhoneNumber(pharmacyId, phone)
                : repo.existsByPharmacyIdAndPhoneNumberAndIdNot(pharmacyId, phone, existingId));
        if (phoneExists) throw new ConflictException("Customer phone number already exists");
        boolean emailExists = email != null && (existingId == null
                ? repo.existsByPharmacyIdAndEmailIgnoreCase(pharmacyId, email)
                : repo.existsByPharmacyIdAndEmailIgnoreCaseAndIdNot(pharmacyId, email, existingId));
        if (emailExists) throw new ConflictException("Customer email already exists");
    }

    private String normalizeEmail(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
