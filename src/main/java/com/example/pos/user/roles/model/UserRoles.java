package com.example.pos.user.roles.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.user.rolepermissions.model.RolePermission;
import com.example.pos.user.staffshifts.model.StaffShifts;
import com.example.pos.user.userbranchrole.model.UserBranchRole;
import com.example.pos.user.users.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@Table(name = "user_roles")
@NoArgsConstructor
@AllArgsConstructor

public class UserRoles extends BaseEntity {

    @Column(unique = true)
    private String roleName;

    @Column(length = 500)
    private String description;

    @Builder.Default
    @OneToMany(mappedBy = "userRoles", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<RolePermission> rolePermission = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "userRoles", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<StaffShifts> staffShifts = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "role", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<UserBranchRole> userBranchRole = new HashSet<>();

}
