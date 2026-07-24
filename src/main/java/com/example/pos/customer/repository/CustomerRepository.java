package com.example.pos.customer.repository;

import com.example.pos.customer.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPhoneNumber(String phoneNumber);
    Optional<Customer> findByEmail(String email);

    @Query("SELECT c FROM Customer c WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(c.phoneNumber) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Customer> search(@Param("q") String q, Pageable pageable);
}
