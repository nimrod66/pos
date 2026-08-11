package com.example.pos.customer.repository;

import java.util.UUID;

import com.example.pos.customer.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByIdAndPharmacyId(UUID id, UUID pharmacyId);

    Page<Customer> findByPharmacyId(UUID pharmacyId, Pageable pageable);

    Optional<Customer> findByPharmacyIdAndPhoneNumber(UUID pharmacyId, String phoneNumber);

    boolean existsByPharmacyIdAndPhoneNumber(UUID pharmacyId, String phoneNumber);

    boolean existsByPharmacyIdAndPhoneNumberAndIdNot(UUID pharmacyId, String phoneNumber, UUID id);

    boolean existsByPharmacyIdAndEmailIgnoreCase(UUID pharmacyId, String email);

    boolean existsByPharmacyIdAndEmailIgnoreCaseAndIdNot(UUID pharmacyId, String email, UUID id);

    @Query("SELECT c FROM Customer c WHERE c.pharmacy.id = :pharmacyId AND "
            + "(LOWER(c.firstName) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(c.phoneNumber) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Customer> searchByPharmacy(@Param("pharmacyId") UUID pharmacyId,
                                    @Param("q") String q,
                                    Pageable pageable);

    Optional<Customer> findByPhoneNumber(String phoneNumber);
    Optional<Customer> findByEmail(String email);

    @Query("SELECT c FROM Customer c WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(c.phoneNumber) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Customer> search(@Param("q") String q, Pageable pageable);
}
