package com.example.pos.user.permissions.service;

import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.user.permissions.dto.PermissionRequestDto;
import com.example.pos.user.permissions.model.Permissions;
import com.example.pos.user.permissions.repository.PermissionsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PermissionsService {

    private final PermissionsRepository permissionsRepository;

    public PermissionsService(PermissionsRepository permissionsRepository) {
        this.permissionsRepository = permissionsRepository;
    }

    public Permissions createPermission(PermissionRequestDto dto) {
        if (permissionsRepository.existsByPermissionName(dto.getPermissionName())) {
            throw new ConflictException("Permission '" + dto.getPermissionName() + "' already exists");
        }
        Permissions permission = new Permissions();
        mapToEntity(dto, permission);
        return permissionsRepository.save(permission);
    }

    @Transactional(readOnly = true)
    public List<Permissions> getAllPermissions() {
        return permissionsRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Permissions> getPermissionsByModule(String moduleName) {
        return permissionsRepository.findByModuleName(moduleName);
    }

    @Transactional(readOnly = true)
    public Permissions getPermissionById(Long id) {
        return permissionsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", id));
    }

    public Permissions updatePermission(Long id, PermissionRequestDto dto) {
        Permissions permission = getPermissionById(id);
        if (permissionsRepository.existsByPermissionNameAndIdNot(dto.getPermissionName(), id)) {
            throw new ConflictException("Permission '" + dto.getPermissionName() + "' already exists");
        }
        mapToEntity(dto, permission);
        return permissionsRepository.save(permission);
    }

    public void deletePermission(Long id) {
        Permissions permission = getPermissionById(id);
        permissionsRepository.delete(permission);
    }

    private void mapToEntity(PermissionRequestDto dto, Permissions permission) {
        permission.setPermissionName(dto.getPermissionName());
        permission.setModuleName(dto.getModuleName());
        permission.setActionName(dto.getActionName());
        permission.setDescription(dto.getDescription());
    }
}
