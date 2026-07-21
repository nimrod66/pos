package com.example.pos.user.rolepermissions.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.user.roles.model.UserRoles;
import com.example.pos.user.permissions.model.Permissions;
import jakarta.persistence.*;
import lombok.*;


@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "role_permissions")
@Getter
@Setter
public class RolePermission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private UserRoles userRoles;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permissions permissions;
}
