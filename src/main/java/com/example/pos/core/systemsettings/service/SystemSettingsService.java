package com.example.pos.core.systemsettings.service;

import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.branch.repository.BranchRepository;
import com.example.pos.core.pharmacy.model.Pharmacy;
import com.example.pos.core.pharmacy.repository.PharmacyRepository;
import com.example.pos.core.systemsettings.dto.SystemSettingsRequestDto;
import com.example.pos.core.systemsettings.model.SystemSettings;
import com.example.pos.core.systemsettings.repository.SystemSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SystemSettingsService {

    private final SystemSettingsRepository settingsRepository;
    private final BranchRepository branchRepository;
    private final PharmacyRepository pharmacyRepository;

    public SystemSettingsService(SystemSettingsRepository settingsRepository,
                                 BranchRepository branchRepository,
                                 PharmacyRepository pharmacyRepository) {
        this.settingsRepository = settingsRepository;
        this.branchRepository = branchRepository;
        this.pharmacyRepository = pharmacyRepository;
    }

    public SystemSettings createSetting(SystemSettingsRequestDto dto) {
        Pharmacy pharmacy = pharmacyRepository.findById(dto.getPharmacyId())
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy", dto.getPharmacyId()));

        Branch branch = null;
        if (dto.getBranchId() != null) {
            branch = branchRepository.findById(dto.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", dto.getBranchId()));
        }

        if (settingsRepository.findSetting(dto.getSettingKey(), dto.getBranchId(), dto.getPharmacyId()).isPresent()) {
            throw new ConflictException("Setting '" + dto.getSettingKey()
                    + "' already exists for this scope");
        }

        SystemSettings settings = new SystemSettings();
        settings.setBranch(branch);
        settings.setPharmacy(pharmacy);
        mapToEntity(dto, settings);
        return settingsRepository.save(settings);
    }

    @Transactional(readOnly = true)
    public List<SystemSettings> getSettingsByPharmacy(Long pharmacyId) {
        return settingsRepository.findByPharmacyId(pharmacyId);
    }

    @Transactional(readOnly = true)
    public List<SystemSettings> getSettingsByBranch(Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
        return settingsRepository.findByPharmacyIdAndBranchId(
                branch.getPharmacy().getId(), branchId);
    }

    @Transactional(readOnly = true)
    public List<SystemSettings> getPharmacyWideSettings(Long pharmacyId) {
        return settingsRepository.findByPharmacyIdAndBranchIsNull(pharmacyId);
    }

    @Transactional(readOnly = true)
    public SystemSettings getSettingById(Long id) {
        return settingsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SystemSetting", id));
    }

    @Transactional(readOnly = true)
    public SystemSettings resolveSetting(String key, Long branchId, Long pharmacyId) {
        SystemSettings override = settingsRepository.findSetting(key, branchId, pharmacyId).orElse(null);
        if (override != null) {
            return override;
        }
        return settingsRepository.findSetting(key, null, pharmacyId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public String resolveSettingValue(String key, Long branchId, Long pharmacyId, String defaultValue) {
        SystemSettings setting = resolveSetting(key, branchId, pharmacyId);
        return setting != null ? setting.getSettingValue() : defaultValue;
    }

    public SystemSettings updateSetting(Long id, SystemSettingsRequestDto dto) {
        SystemSettings settings = getSettingById(id);

        if (dto.getBranchId() != null) {
            if (settings.getBranch() == null || !settings.getBranch().getId().equals(dto.getBranchId())) {
                Branch branch = branchRepository.findById(dto.getBranchId())
                        .orElseThrow(() -> new ResourceNotFoundException("Branch", dto.getBranchId()));
                settings.setBranch(branch);
            }
        } else {
            settings.setBranch(null);
        }

        if (!settings.getPharmacy().getId().equals(dto.getPharmacyId())) {
            Pharmacy pharmacy = pharmacyRepository.findById(dto.getPharmacyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Pharmacy", dto.getPharmacyId()));
            settings.setPharmacy(pharmacy);
        }

        mapToEntity(dto, settings);
        return settingsRepository.save(settings);
    }

    public void deleteSetting(Long id) {
        SystemSettings settings = getSettingById(id);
        settingsRepository.delete(settings);
    }

    private void mapToEntity(SystemSettingsRequestDto dto, SystemSettings settings) {
        settings.setSettingKey(dto.getSettingKey());
        settings.setSettingValue(dto.getSettingValue());
        settings.setDescription(dto.getDescription());
    }
}
