package com.example.pos.core.systemsettings.repository;

import com.example.pos.core.systemsettings.model.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Long> {

    @Query("SELECT s FROM SystemSettings s WHERE s.settingKey = :key " +
           "AND (:branchId IS NULL AND s.branch IS NULL OR s.branch.id = :branchId) " +
           "AND s.pharmacy.id = :pharmacyId")
    Optional<SystemSettings> findSetting(String key, Long branchId, Long pharmacyId);

    boolean existsBySettingKeyAndPharmacyIdAndBranch(String key, Long pharmacyId, Object branch);

    List<SystemSettings> findByPharmacyIdAndBranchIsNull(Long pharmacyId);

    List<SystemSettings> findByPharmacyIdAndBranchId(Long pharmacyId, Long branchId);

    List<SystemSettings> findByPharmacyId(Long pharmacyId);
}
