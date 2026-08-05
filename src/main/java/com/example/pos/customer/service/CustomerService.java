package com.example.pos.customer.service;

import com.example.pos.common.annotation.Auditable;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.customer.dto.CustomerRequestDto;
import com.example.pos.customer.model.Customer;
import com.example.pos.customer.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository repo;
    public CustomerService(CustomerRepository repo) { this.repo = repo; }

    @Auditable(action = "CREATE_CUSTOMER", entity = "Customer")
    public Customer create(CustomerRequestDto dto) {
        Customer c = Customer.builder()
                .firstName(dto.getFirstName()).lastName(dto.getLastName())
                .phoneNumber(dto.getPhoneNumber()).email(dto.getEmail())
                .address(dto.getAddress()).notes(dto.getNotes()).build();
        return repo.save(c);
    }

    @Transactional(readOnly = true)
    public Page<Customer> getAll(Pageable pageable) { return repo.findAll(pageable); }

    @Transactional(readOnly = true)
    public Page<Customer> search(String q, Pageable pageable) { return repo.search(q, pageable); }

    @Transactional(readOnly = true)
    public Customer getById(UUID id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    @Transactional(readOnly = true)
    public Customer findByPhone(String phone) {
        return repo.findByPhoneNumber(phone).orElseThrow(() -> new ResourceNotFoundException("Customer with phone " + phone));
    }

    @Auditable(action = "UPDATE_CUSTOMER", entity = "Customer")
    public Customer update(UUID id, CustomerRequestDto dto) {
        Customer c = getById(id);
        c.setFirstName(dto.getFirstName()); c.setLastName(dto.getLastName());
        c.setPhoneNumber(dto.getPhoneNumber()); c.setEmail(dto.getEmail());
        c.setAddress(dto.getAddress()); c.setNotes(dto.getNotes());
        return repo.save(c);
    }

    public Customer addLoyaltyPoints(UUID id, int points) {
        Customer c = getById(id);
        c.setLoyaltyPoints(c.getLoyaltyPoints() + points);
        return repo.save(c);
    }
}
