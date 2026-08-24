package com.example.pos.core.seed;

import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.branch.repository.BranchRepository;
import com.example.pos.core.pharmacy.model.Pharmacy;
import com.example.pos.core.pharmacy.repository.PharmacyRepository;
import com.example.pos.user.permissions.model.Permissions;
import com.example.pos.user.permissions.repository.PermissionsRepository;
import com.example.pos.user.rolepermissions.model.RolePermission;
import com.example.pos.user.rolepermissions.repository.RolePermissionRepository;
import com.example.pos.user.roles.model.UserRoles;
import com.example.pos.user.roles.repository.UserRolesRepository;
import com.example.pos.user.userbranchrole.model.UserBranchRole;
import com.example.pos.user.userbranchrole.repository.UserBranchRoleRepository;
import com.example.pos.user.users.model.User;
import com.example.pos.user.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private PharmacyRepository pharmacyRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserRolesRepository rolesRepository;
    @Mock
    private PermissionsRepository permissionsRepository;
    @Mock
    private RolePermissionRepository rolePermissionRepository;
    @Mock
    private UserBranchRoleRepository userBranchRoleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private DataInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new DataInitializer(
                pharmacyRepository,
                branchRepository,
                userRepository,
                rolesRepository,
                permissionsRepository,
                rolePermissionRepository,
                userBranchRoleRepository,
                passwordEncoder);
    }

    @Test
    void addsAllRoleAccountsWhenTheDemoPharmacyAlreadyExists() {
        Pharmacy pharmacy = Pharmacy.builder()
                .name("Demo Pharmacy Ltd")
                .email("admin@demopharmacy.co.ke")
                .build();
        setId(pharmacy, "pharmacy");
        Branch branch = Branch.builder()
                .branchName("Main Branch")
                .branchCode("MAIN")
                .pharmacy(pharmacy)
                .status(Branch.Status.ACTIVE)
                .build();
        setId(branch, "branch");

        Map<String, UserRoles> roles = Set.of(
                        "OWNER", "BRANCH_MANAGER", "PHARMACIST", "CASHIER", "STORE_KEEPER",
                        "PHARMACY_TECHNICIAN")
                .stream()
                .collect(Collectors.toMap(roleName -> roleName, roleName -> {
                    UserRoles role = UserRoles.builder().roleName(roleName).build();
                    setId(role, "role-" + roleName);
                    return role;
                }));

        when(pharmacyRepository.findByEmail("admin@demopharmacy.co.ke"))
                .thenReturn(Optional.of(pharmacy));
        when(branchRepository.findByPharmacyId(pharmacy.getId())).thenReturn(List.of(branch));
        when(permissionsRepository.existsByPermissionName(anyString())).thenReturn(true);
        when(permissionsRepository.findByPermissionName(anyString())).thenAnswer(invocation -> {
            String code = invocation.getArgument(0);
            Permissions permission = Permissions.builder().permissionName(code).build();
            setId(permission, "permission-" + code);
            return Optional.of(permission);
        });
        when(rolesRepository.existsByRoleName(anyString())).thenReturn(true);
        when(rolesRepository.findByRoleName(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(roles.get(invocation.getArgument(0))));
        when(rolePermissionRepository.findByUserRolesAndPermissionsId(any(), any()))
                .thenReturn(Optional.of(RolePermission.builder().build()));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation ->
                "encoded:" + invocation.getArgument(0));

        AtomicInteger userSequence = new AtomicInteger();
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            setId(user, "user-" + userSequence.incrementAndGet());
            return user;
        });

        initializer.run();

        ArgumentCaptor<User> users = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(6)).save(users.capture());
        assertThat(users.getAllValues())
                .extracting(User::getEmail)
                .containsExactlyInAnyOrder(
                        "admin@demo.com",
                        "manager@demo.com",
                        "pharmacist@demo.com",
                        "cashier@demo.com",
                        "storekeeper@demo.com",
                        "technician@demo.com");

        ArgumentCaptor<UserBranchRole> assignments = ArgumentCaptor.forClass(UserBranchRole.class);
        verify(userBranchRoleRepository, times(6)).save(assignments.capture());
        Map<String, Set<String>> assignedRoles = assignments.getAllValues().stream()
                .collect(Collectors.groupingBy(
                        assignment -> assignment.getUser().getEmail(),
                        Collectors.mapping(
                                assignment -> assignment.getRole().getRoleName(),
                                Collectors.toSet())));
        assertThat(assignedRoles.get("admin@demo.com")).containsExactly("OWNER");
        assertThat(assignedRoles.get("manager@demo.com")).containsExactly("BRANCH_MANAGER");
        assertThat(assignedRoles.get("pharmacist@demo.com")).containsExactly("PHARMACIST");
        assertThat(assignedRoles.get("cashier@demo.com")).containsExactly("CASHIER");
        assertThat(assignedRoles.get("storekeeper@demo.com")).containsExactly("STORE_KEEPER");
        assertThat(assignedRoles.get("technician@demo.com"))
                .containsExactly("PHARMACY_TECHNICIAN");
    }

    private static void setId(Object entity, String source) {
        ReflectionTestUtils.setField(
                entity,
                "id",
                UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8)));
    }
}
