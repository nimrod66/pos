package com.example.pos.insurance.repository;

import com.example.pos.insurance.model.InsuranceMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsuranceMemberRepository extends JpaRepository<InsuranceMember, Long> {
    Optional<InsuranceMember> findByMembershipNumber(String membershipNumber);
    List<InsuranceMember> findByInsurerId(Long insurerId);
    List<InsuranceMember> findByInsurerIdAndStatus(Long insurerId, InsuranceMember.MemberStatus status);
    Optional<InsuranceMember> findByMembershipNumberAndInsurerId(String membershipNumber, Long insurerId);
}
