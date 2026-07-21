package com.example.pos.user.permissions.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.user.rolepermissions.model.RolePermission;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "permissions")
@Builder

public class Permissions extends BaseEntity {

    @Builder.Default
    @OneToMany(mappedBy = "permissions", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<RolePermission> rolePermission = new HashSet<>();

    @Column(unique = true)
    private String permissionName;
    private String moduleName;
    private String actionName;
    private String description;
}
