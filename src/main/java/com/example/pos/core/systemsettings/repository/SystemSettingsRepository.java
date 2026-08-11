package com.example.pos.core.systemsettings.repository;

import java.util.UUID;

import com.example.pos.core.systemsettings.model.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SystemSettingsRepository extends JpaRepository<SystemSettings, UUID> {

    @Query("SELECT s FROM SystemSettings s WHERE s.settingKey = :key " +
           "AND (:branchId IS NULL AND s.branch IS NULL OR s.branch.id = :branchId) " +
           "AND s.pharmacy.id = :pharmacyId")
    Optional<SystemSettings> findSetting(String key, UUID branchId, UUID pharmacyId);

    boolean existsBySettingKeyAndPharmacyIdAndBranch(String key, UUID pharmacyId, Object branch);

    List<SystemSettings> findByPharmacyIdAndBranchIsNull(UUID pharmacyId);

    List<SystemSettings> findByPharmacyIdAndBranchId(UUID pharmacyId, UUID branchId);

    List<SystemSettings> findByPharmacyId(UUID pharmacyId);

    Optional<SystemSettings> findByIdAndPharmacyId(UUID id, UUID pharmacyId);
}
