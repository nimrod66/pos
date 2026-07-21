package com.example.pos.customer.service;

import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.customer.dto.CustomerRequestDto;
import com.example.pos.customer.model.Customer;
import com.example.pos.customer.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository repo;
    public CustomerService(CustomerRepository repo) { this.repo = repo; }

    public Customer create(CustomerRequestDto dto) {
        Customer c = Customer.builder()
                .firstName(dto.getFirstName()).lastName(dto.getLastName())
                .phoneNumber(dto.getPhoneNumber()).email(dto.getEmail())
                .address(dto.getAddress()).notes(dto.getNotes()).build();
        return repo.save(c);
    }

    @Transactional(readOnly = true)
    public List<Customer> getAll() { return repo.findAll(); }

    @Transactional(readOnly = true)
    public Customer getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    @Transactional(readOnly = true)
    public Customer findByPhone(String phone) {
        return repo.findByPhoneNumber(phone).orElseThrow(() -> new ResourceNotFoundException("Customer with phone " + phone));
    }

    public Customer update(Long id, CustomerRequestDto dto) {
        Customer c = getById(id);
        c.setFirstName(dto.getFirstName()); c.setLastName(dto.getLastName());
        c.setPhoneNumber(dto.getPhoneNumber()); c.setEmail(dto.getEmail());
        c.setAddress(dto.getAddress()); c.setNotes(dto.getNotes());
        return repo.save(c);
    }

    public Customer addLoyaltyPoints(Long id, int points) {
        Customer c = getById(id);
        c.setLoyaltyPoints(c.getLoyaltyPoints() + points);
        return repo.save(c);
    }
}
