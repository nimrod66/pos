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

                if (role.getRoleName() != null) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleName()));
                }
                if (role.getRolePermission() != null) {
                    for (RolePermission rp : role.getRolePermission()) {
                        if (rp.getPermissions() != null) {
                            String perm = "PERM_" + rp.getPermissions().getPermissionName();
                            authorities.add(new SimpleGrantedAuthority(perm));
                        }
                    }
                }
            }
        }

        boolean active = user.getStatus() == User.Status.ACTIVE;
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), user.getPasswordHash(),
                active, true, true, !user.getStatus().equals(User.Status.INACTIVE),
                authorities);
    }
}
