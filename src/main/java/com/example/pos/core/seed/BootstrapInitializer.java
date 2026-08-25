package com.example.pos.core.seed;

import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.branch.repository.BranchRepository;
import com.example.pos.core.pharmacy.model.Pharmacy;
import com.example.pos.core.pharmacy.repository.PharmacyRepository;
import com.example.pos.security.auth.PermissionCodes;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Prepares a production-shaped database for first use WITHOUT demo data:
 * reference roles/permissions, one pharmacy, one branch, and a single
 * bootstrap owner sourced from configuration.
 *
 * Skipped when demo seeding is enabled - the demo initializer owns that
 * path. Never runs against a database that already has users.
 */
@Slf4j
@Component
@Order(0)
public class BootstrapInitializer implements CommandLineRunner {

    private static final String DEFAULT_BRANCH_CODE = "MAIN";

    private final PharmacyRepository pharmacyRepo;
    private final BranchRepository branchRepo;
    private final UserRepository userRepo;
    private final UserRolesRepository rolesRepo;
    private final PermissionsRepository permRepo;
    private final RolePermissionRepository rolePermRepo;
    private final UserBranchRoleRepository ubrRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${pos.bootstrap.enabled:true}")
    private boolean enabled;

    @Value("${pos.bootstrap.admin-email:admin@pharmacy.local}")
    private String adminEmail;

    @Value("${pos.bootstrap.admin-password:}")
    private String adminPassword;

    @Value("${pos.bootstrap.pharmacy-name:My Pharmacy}")
    private String pharmacyName;

    public BootstrapInitializer(PharmacyRepository pharmacyRepo,
                                BranchRepository branchRepo,
                                UserRepository userRepo,
                                UserRolesRepository rolesRepo,
                                PermissionsRepository permRepo,
                                RolePermissionRepository rolePermRepo,
                                UserBranchRoleRepository ubrRepo,
                                PasswordEncoder passwordEncoder) {
        this.pharmacyRepo = pharmacyRepo;
        this.branchRepo = branchRepo;
        this.userRepo = userRepo;
        this.rolesRepo = rolesRepo;
        this.permRepo = permRepo;
        this.rolePermRepo = rolePermRepo;
        this.ubrRepo = ubrRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        Map<String, String> roleNames = Map.of(
                "OWNER", "Full system access, pharmacy owner",
                "BRANCH_MANAGER", "Manage branch operations",
                "PHARMACIST", "Dispense prescriptions, manage medicines",
                "CASHIER", "Process sales and payments",
                "STORE_KEEPER", "Manage inventory and stock",
                "PHARMACY_TECHNICIAN", "Assist dispensing, sales, and stock under supervision"
        );

        for (String code : PermissionCodes.ALL) {
            int separator = code.indexOf('.');
            String module = code.substring(0, separator);
            String action = code.substring(separator + 1);
            if (!permRepo.existsByPermissionName(code)) {
                permRepo.save(Permissions.builder()
                        .permissionName(code)
                        .moduleName(module)
                        .actionName(action)
                        .description(action.replace('.', ' '))
                        .build());
            }
        }

        for (Map.Entry<String, String> entry : roleNames.entrySet()) {
            if (!rolesRepo.existsByRoleName(entry.getKey())) {
                rolesRepo.save(UserRoles.builder()
                        .roleName(entry.getKey())
                        .description(entry.getValue())
                        .build());
            }
        }

        for (Map.Entry<String, List<String>> bundle : PermissionCodes.ROLE_BUNDLES.entrySet()) {
            UserRoles role = rolesRepo.findByRoleName(bundle.getKey()).orElseThrow();
            for (String permissionCode : bundle.getValue()) {
                Permissions permission = permRepo.findByPermissionName(permissionCode).orElseThrow();
                if (rolePermRepo.findByUserRolesAndPermissionsId(role, permission.getId()).isEmpty()) {
                    rolePermRepo.save(RolePermission.builder()
                            .userRoles(role)
                            .permissions(permission)
                            .build());
                }
            }
        }

        if (userRepo.count() > 0) {
            log.info("Bootstrap skipped: users already exist.");
            return;
        }

        Optional<Pharmacy> existing = pharmacyRepo.findAll().stream().findFirst();
        Pharmacy pharmacy = existing.orElseGet(() -> pharmacyRepo.save(Pharmacy.builder()
                .name(pharmacyName)
                .address("")
                .phoneNumber("")
                .email(adminEmail)
                .licenseNumber("")
                .kraPin("")
                .build()));

        Branch branch = branchRepo.findByPharmacyId(pharmacy.getId()).stream()
                .filter(candidate -> DEFAULT_BRANCH_CODE.equalsIgnoreCase(candidate.getBranchCode()))
                .findFirst()
                .orElseGet(() -> branchRepo.save(Branch.builder()
                        .branchName("Main Branch")
                        .branchCode(DEFAULT_BRANCH_CODE)
                        .phoneNumber("")
                        .email(adminEmail)
                        .location("")
                        .pharmacy(pharmacy)
                        .status(Branch.Status.ACTIVE)
                        .build()));

        String password = adminPassword;
        boolean generated = false;
        if (password == null || password.isBlank() || password.length() < 8) {
            password = generatePassword();
            generated = true;
        }

        User owner = userRepo.save(User.builder()
                .firstName("Pharmacy")
                .lastName("Owner")
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(password))
                .phoneNumber("")
                .branch(branch)
                .status(User.Status.ACTIVE)
                .build());

        UserRoles ownerRole = rolesRepo.findByRoleName("OWNER").orElseThrow();
        ubrRepo.save(UserBranchRole.builder()
                .user(owner)
                .branch(branch)
                .role(ownerRole)
                .assignedBy(owner)
                .assignedAt(LocalDateTime.now())
                .build());

        if (generated) {
            log.warn("BOOTSTRAP OWNER CREATED: {} with generated password '{}'. "
                    + "Sign in and change it immediately, or set POS_BOOTSTRAP_ADMIN_PASSWORD.",
                    adminEmail, password);
        } else {
            log.info("Bootstrap owner {} created.", adminEmail);
        }
    }

    private String generatePassword() {
        var secureRandom = new java.security.SecureRandom();
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder(14);
        for (int i = 0; i < 14; i++) {
            sb.append(alphabet.charAt(secureRandom.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
