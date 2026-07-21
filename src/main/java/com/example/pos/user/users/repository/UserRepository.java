package com.example.pos.user.users.repository;

import com.example.pos.user.users.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByPhoneNumber(String phoneNumber);

    List<User> findByBranchId(Long branchId);

    Optional<User> findByEmailAndStatus(String email, User.Status status);
}
