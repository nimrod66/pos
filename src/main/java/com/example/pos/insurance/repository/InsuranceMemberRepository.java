package com.example.pos.insurance.repository;

import java.util.UUID;

import com.example.pos.insurance.model.InsuranceMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsuranceMemberRepository extends JpaRepository<InsuranceMember, UUID> {
    Optional<InsuranceMember> findByMembershipNumber(String membershipNumber);
    List<InsuranceMember> findByInsurerId(UUID insurerId);
    List<InsuranceMember> findByInsurerIdAndStatus(UUID insurerId, InsuranceMember.MemberStatus status);
    Optional<InsuranceMember> findByMembershipNumberAndInsurerId(String membershipNumber, UUID insurerId);
}
