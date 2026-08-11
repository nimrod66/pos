package com.example.pos.user.users.repository;

import java.util.UUID;

import com.example.pos.user.users.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = {"branch", "branch.pharmacy"})
    @Query("SELECT user FROM User user WHERE user.id = :id")
    Optional<User> findContextById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"branch", "branch.pharmacy"})
    Optional<User> findByIdAndBranchPharmacyId(UUID id, UUID pharmacyId);

    @EntityGraph(attributePaths = {"branch", "branch.pharmacy"})
    Page<User> findByBranchPharmacyId(UUID pharmacyId, Pageable pageable);

    @EntityGraph(attributePaths = {"branch", "branch.pharmacy"})
    Page<User> findByBranchIdAndBranchPharmacyId(UUID branchId, UUID pharmacyId, Pageable pageable);

    @EntityGraph(attributePaths = {"branch", "branch.pharmacy"})
    @Query("SELECT u FROM User u WHERE u.branch.pharmacy.id = :pharmacyId AND "
            + "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<User> searchByPharmacy(@Param("pharmacyId") UUID pharmacyId,
                                @Param("q") String q,
                                Pageable pageable);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByPhoneNumber(String phoneNumber);

    List<User> findByBranchId(UUID branchId);

    Optional<User> findByEmailAndStatus(String email, User.Status status);

    @Query("SELECT u FROM User u WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<User> search(@Param("q") String q, Pageable pageable);
}
