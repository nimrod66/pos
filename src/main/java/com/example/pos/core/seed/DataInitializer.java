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
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private final PharmacyRepository pharmacyRepo;
    private final BranchRepository branchRepo;
    private final UserRepository userRepo;
    private final UserRolesRepository rolesRepo;
    private final PermissionsRepository permRepo;
    private final RolePermissionRepository rolePermRepo;
    private final UserBranchRoleRepository ubrRepo;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(PharmacyRepository pharmacyRepo, BranchRepository branchRepo,
                           UserRepository userRepo, UserRolesRepository rolesRepo,
                           PermissionsRepository permRepo, RolePermissionRepository rolePermRepo,
                           UserBranchRoleRepository ubrRepo, PasswordEncoder passwordEncoder) {
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
        if (pharmacyRepo.count() > 0) {
            return;
        }

        log.info("Seeding demo data...");

        Pharmacy pharmacy = Pharmacy.builder()
                .name("Demo Pharmacy Ltd")
                .address("123 Healthcare Street, Nairobi")
                .phoneNumber("0712345678")
                .email("admin@demopharmacy.co.ke")
                .licenseNumber("PPB-DEMO-001")
                .kraPin("P051234567A")
                .build();
        pharmacy = pharmacyRepo.save(pharmacy);

        Branch branch = Branch.builder()
                .branchName("Main Branch")
                .branchCode("MAIN")
                .phoneNumber("0712345678")
                .email("main@demopharmacy.co.ke")
                .location("Ground Floor, Healthcare Plaza")
                .pharmacy(pharmacy)
                .status(Branch.Status.ACTIVE)
                .build();
        branch = branchRepo.save(branch);

        Map<String, String> roleNames = Map.of(
                "OWNER", "Full system access, pharmacy owner",
                "PLATFORM_ADMIN", "Platform-level admin, manage pharmacies",
                "BRANCH_MANAGER", "Manage branch operations",
                "PHARMACIST", "Dispense prescriptions, manage medicines",
                "CASHIER", "Process sales and payments",
                "STORE_KEEPER", "Manage inventory and stock"
        );

        Map<String, List<String>> permsByModule = new java.util.LinkedHashMap<>();
        permsByModule.put("SALES", List.of("CREATE_SALE", "VIEW_SALE", "VOID_SALE", "PROCESS_RETURN"));
        permsByModule.put("INVENTORY", List.of("VIEW_STOCK", "ADJUST_STOCK", "RECEIVE_STOCK", "TRANSFER_STOCK"));
        permsByModule.put("PURCHASE", List.of("CREATE_PO", "APPROVE_PO", "VIEW_PO", "RECEIVE_GOODS"));
        permsByModule.put("MEDICINES", List.of("CREATE_MEDICINE", "EDIT_MEDICINE", "DELETE_MEDICINE", "VIEW_MEDICINE"));
        permsByModule.put("USERS", List.of("CREATE_USER", "EDIT_USER", "DELETE_USER", "VIEW_USER"));
        permsByModule.put("ROLES", List.of("MANAGE_ROLES", "MANAGE_PERMISSIONS"));
        permsByModule.put("REPORTS", List.of("VIEW_REPORTS", "EXPORT_REPORTS"));
        permsByModule.put("FINANCE", List.of("VIEW_FINANCE", "MANAGE_EXPENSES", "MANAGE_CASH_DRAWER"));
        permsByModule.put("PRESCRIPTIONS", List.of("CREATE_PRESCRIPTION", "DISPENSE", "VIEW_PRESCRIPTION"));
        permsByModule.put("SETTINGS", List.of("MANAGE_SETTINGS"));
        permsByModule.put("PHARMACY", List.of("MANAGE_PHARMACY", "MANAGE_BRANCH"));
        permsByModule.put("COMPLIANCE", List.of("VIEW_AUDIT", "MANAGE_ETIMS", "MANAGE_CONTROLLED"));

        for (Map.Entry<String, List<String>> entry : permsByModule.entrySet()) {
            String module = entry.getKey();
            for (String action : entry.getValue()) {
                if (!permRepo.existsByPermissionName(module + "_" + action)) {
                    Permissions perm = Permissions.builder()
                            .permissionName(module + "_" + action)
                            .moduleName(module)
                            .actionName(action)
                            .description(action.replace("_", " "))
                            .build();
                    permRepo.save(perm);
                }
            }
        }

        for (Map.Entry<String, String> entry : roleNames.entrySet()) {
            if (!rolesRepo.existsByRoleName(entry.getKey())) {
                UserRoles role = UserRoles.builder()
                        .roleName(entry.getKey())
                        .description(entry.getValue())
                        .build();
                rolesRepo.save(role);
            }
        }

        UserRoles ownerRole = rolesRepo.findByRoleName("OWNER").orElseThrow();
        List<Permissions> allPermissions = permRepo.findAll();
        for (Permissions p : allPermissions) {
            RolePermission rp = RolePermission.builder()
                    .userRoles(ownerRole)
                    .permissions(p)
                    .build();
            rolePermRepo.save(rp);
        }

        assignRoleToModulePerms("BRANCH_MANAGER", List.of("SALES", "INVENTORY", "PURCHASE", "MEDICINES", "REPORTS", "FINANCE", "PRESCRIPTIONS", "COMPLIANCE"));
        assignRoleToModulePerms("PHARMACIST", List.of("SALES", "MEDICINES", "PRESCRIPTIONS", "INVENTORY"));
        assignRoleToModulePerms("CASHIER", List.of("SALES"));
        assignRoleToModulePerms("STORE_KEEPER", List.of("INVENTORY", "PURCHASE", "MEDICINES"));

        User admin = User.builder()
                .firstName("System")
                .lastName("Admin")
                .email("admin@demo.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .phoneNumber("0700000000")
                .branch(branch)
                .status(User.Status.ACTIVE)
                .lastLogin(LocalDateTime.now())
                .build();
        admin = userRepo.save(admin);

        UserBranchRole ubr = UserBranchRole.builder()
                .user(admin)
                .branch(branch)
                .role(ownerRole)
                .assignedBy(admin)
                .assignedAt(LocalDateTime.now())
                .build();
        ubrRepo.save(ubr);

        log.info("Demo data seeded. Login: admin@demo.com / admin123");
    }

    private void assignRoleToModulePerms(String roleName, List<String> modules) {
        UserRoles role = rolesRepo.findByRoleName(roleName).orElseThrow();
        for (String module : modules) {
            List<Permissions> perms = permRepo.findByModuleName(module);
            for (Permissions p : perms) {
                if (rolePermRepo.findByUserRolesAndPermissionsId(role, p.getId()).isEmpty()) {
                    RolePermission rp = RolePermission.builder()
                            .userRoles(role)
                            .permissions(p)
                            .build();
                    rolePermRepo.save(rp);
                }
            }
        }
    }
}
