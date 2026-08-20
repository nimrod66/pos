package com.example.pos.security.auth;

import com.example.pos.user.rolepermissions.model.RolePermission;
import com.example.pos.user.roles.model.UserRoles;
import com.example.pos.user.userbranchrole.model.UserBranchRole;
import com.example.pos.user.users.model.User;
import com.example.pos.user.users.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        if (user.getUserBranchRole() != null) {
            for (UserBranchRole branchRole : user.getUserBranchRole()) {
                UserRoles role = branchRole.getRole();
                if (role == null) continue;
                boolean activeBranchRole = user.getBranch() != null
                        && branchRole.getBranch() != null
                        && user.getBranch().getId().equals(branchRole.getBranch().getId());
                boolean pharmacyOwnerRole = "OWNER".equals(role.getRoleName())
                        && user.getBranch() != null
                        && user.getBranch().getPharmacy() != null
                        && branchRole.getBranch() != null
                        && branchRole.getBranch().getPharmacy() != null
                        && user.getBranch().getPharmacy().getId().equals(
                                branchRole.getBranch().getPharmacy().getId());
                if (!activeBranchRole && !pharmacyOwnerRole) continue;

                if (role.getRoleName() != null) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleName()));
                }
                if (role.getRolePermission() != null) {
                    for (RolePermission rp : role.getRolePermission()) {
                        if (rp.getPermissions() != null) {
                            authorities.add(new SimpleGrantedAuthority(rp.getPermissions().getPermissionName()));
                        }
                    }
                }
            }
        }

        boolean active = user.getStatus() == User.Status.ACTIVE;
        return new UserDetailsImpl(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                active,
                authorities);
    }
}
