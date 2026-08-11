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
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@Order(1)
@ConditionalOnProperty(name = "pos.seed.demo-enabled", havingValue = "true")
public class DataInitializer implements CommandLineRunner {

    private static final String DEMO_PHARMACY_EMAIL = "admin@demopharmacy.co.ke";
    private static final String DEMO_BRANCH_CODE = "MAIN";

    private static final List<DemoAccount> DEMO_ACCOUNTS = List.of(
            new DemoAccount("System", "Admin", "admin@demo.com", "admin123",
                    "0700000000", List.of("OWNER")),
            new DemoAccount("Branch", "Manager", "manager@demo.com", "manager123",
                    "0700000001", List.of("BRANCH_MANAGER")),
            new DemoAccount("Duty", "Pharmacist", "pharmacist@demo.com", "pharmacist123",
                    "0700000002", List.of("PHARMACIST")),
            new DemoAccount("Main", "Cashier", "cashier@demo.com", "cashier123",
                    "0700000003", List.of("CASHIER")),
            new DemoAccount("Store", "Keeper", "storekeeper@demo.com", "stock1234",
                    "0700000004", List.of("STORE_KEEPER")),
            new DemoAccount("Pharmacy", "Technician", "technician@demo.com", "tech12345",
                    "0700000005", List.of("CASHIER", "STORE_KEEPER")));

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
        log.info("Ensuring local demo data is available...");

        Map<String, String> roleNames = Map.of(
                "OWNER", "Full system access, pharmacy owner",
                "BRANCH_MANAGER", "Manage branch operations",
                "PHARMACIST", "Dispense prescriptions, manage medicines",
                "CASHIER", "Process sales and payments",
                "STORE_KEEPER", "Manage inventory and stock"
        );

        for (String code : PermissionCodes.ALL) {
            int separator = code.indexOf('.');
            String module = code.substring(0, separator);
            String action = code.substring(separator + 1);
            if (!permRepo.existsByPermissionName(code)) {
                Permissions perm = Permissions.builder()
                        .permissionName(code)
                        .moduleName(module)
                        .actionName(action)
                        .description(action.replace('.', ' '))
                        .build();
                permRepo.save(perm);
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

        for (Map.Entry<String, List<String>> bundle : PermissionCodes.ROLE_BUNDLES.entrySet()) {
            assignPermissions(bundle.getKey(), bundle.getValue());
        }

        Optional<Pharmacy> existingDemoPharmacy = pharmacyRepo.findByEmail(DEMO_PHARMACY_EMAIL);
        if (existingDemoPharmacy.isEmpty() && pharmacyRepo.count() > 0) {
            log.warn("Demo seeding is enabled, but this database already belongs to another pharmacy. "
                    + "Skipping demo users to avoid changing tenant data.");
            return;
        }

        Pharmacy pharmacy = existingDemoPharmacy.orElseGet(this::createDemoPharmacy);
        Branch branch = branchRepo.findByPharmacyId(pharmacy.getId()).stream()
                .filter(candidate -> DEMO_BRANCH_CODE.equalsIgnoreCase(candidate.getBranchCode()))
                .findFirst()
                .orElseGet(() -> createDemoBranch(pharmacy));

        DemoAccount ownerAccount = DEMO_ACCOUNTS.getFirst();
        User owner = ensureDemoUser(ownerAccount, branch);
        assignRoles(owner, branch, owner, ownerAccount.roles());

        for (DemoAccount account : DEMO_ACCOUNTS.subList(1, DEMO_ACCOUNTS.size())) {
            User user = ensureDemoUser(account, branch);
            assignRoles(user, branch, owner, account.roles());
        }

        log.info("Demo data ready. {} role-based login accounts are available.", DEMO_ACCOUNTS.size());
    }

    private Pharmacy createDemoPharmacy() {
        return pharmacyRepo.save(Pharmacy.builder()
                .name("Demo Pharmacy Ltd")
                .address("123 Healthcare Street, Nairobi")
                .phoneNumber("0712345678")
                .email(DEMO_PHARMACY_EMAIL)
                .licenseNumber("PPB-DEMO-001")
                .kraPin("P051234567A")
                .build());
    }

    private Branch createDemoBranch(Pharmacy pharmacy) {
        return branchRepo.save(Branch.builder()
                .branchName("Main Branch")
                .branchCode(DEMO_BRANCH_CODE)
                .phoneNumber("0712345678")
                .email("main@demopharmacy.co.ke")
                .location("Ground Floor, Healthcare Plaza")
                .pharmacy(pharmacy)
                .status(Branch.Status.ACTIVE)
                .build());
    }

    private User ensureDemoUser(DemoAccount account, Branch branch) {
        return userRepo.findByEmail(account.email()).orElseGet(() -> userRepo.save(User.builder()
                .firstName(account.firstName())
                .lastName(account.lastName())
                .email(account.email())
                .passwordHash(passwordEncoder.encode(account.password()))
                .phoneNumber(account.phoneNumber())
                .branch(branch)
                .status(User.Status.ACTIVE)
                .build()));
    }

    private void assignRoles(User user, Branch branch, User assignedBy, List<String> roleNames) {
        for (String roleName : roleNames) {
            UserRoles role = rolesRepo.findByRoleName(roleName).orElseThrow();
            if (ubrRepo.existsByUserIdAndBranchIdAndRoleId(
                    user.getId(), branch.getId(), role.getId())) {
                continue;
            }
            ubrRepo.save(UserBranchRole.builder()
                    .user(user)
                    .branch(branch)
                    .role(role)
                    .assignedBy(assignedBy)
                    .assignedAt(LocalDateTime.now())
                    .build());
        }
    }

    private void assignPermissions(String roleName, List<String> permissionCodes) {
        UserRoles role = rolesRepo.findByRoleName(roleName).orElseThrow();
        for (String permissionCode : permissionCodes) {
            Permissions permission = permRepo.findByPermissionName(permissionCode).orElseThrow();
            if (rolePermRepo.findByUserRolesAndPermissionsId(role, permission.getId()).isEmpty()) {
                RolePermission rp = RolePermission.builder()
                        .userRoles(role)
                        .permissions(permission)
                        .build();
                rolePermRepo.save(rp);
            }
        }
    }

    private record DemoAccount(String firstName, String lastName, String email, String password,
                               String phoneNumber, List<String> roles) {
    }
}
