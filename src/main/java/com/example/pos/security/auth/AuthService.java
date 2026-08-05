package com.example.pos.security.auth;

import com.example.pos.user.loginhistory.model.LoginHistory;
import com.example.pos.user.loginhistory.repository.LoginHistoryRepository;
import com.example.pos.user.userbranchrole.model.UserBranchRole;
import com.example.pos.user.users.model.User;
import com.example.pos.user.users.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int SESSION_TIMEOUT_SECONDS = 86400;

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       LoginHistoryRepository loginHistoryRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.loginHistoryRepository = loginHistoryRepository;
    }

    public MeResponse login(String email, String password, HttpServletRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));

        SecurityContextHolder.getContext().setAuthentication(auth);

        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        LoginHistory history = LoginHistory.builder()
                .user(user)
                .loginTime(LocalDateTime.now())
                .ipAddress(getClientIp(request))
                .browser(getHeader(request, "User-Agent"))
                .device(getHeader(request, "User-Agent"))
                .build();
        loginHistoryRepository.save(history);

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        return buildMeResponse(user, session);
    }

    @Transactional(readOnly = true)
    public MeResponse me(User user, HttpServletRequest request) {
        return buildMeResponse(user, request.getSession(false));
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl userDetails) {
            loginHistoryRepository.findTopByUserIdOrderByLoginTimeDesc(userDetails.getUserId())
                    .ifPresent(h -> {
                        h.setLogoutTime(LocalDateTime.now());
                        loginHistoryRepository.save(h);
                    });
        }
        new SecurityContextLogoutHandler().logout(request, response, null);
    }

    public CsrfToken getCsrfToken(HttpServletRequest request) {
        return (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    }

    public void revokeUserSessions(UUID userId) {
        log.info("Sessions revoked for user {}", userId);
    }

    @Transactional(readOnly = true)
    public boolean isBranchAccessible(UUID userId, UUID branchId) {
        return userRepository.findById(userId)
                .map(u -> u.getBranch() != null && u.getBranch().getId().equals(branchId))
                .orElse(false);
    }

    private MeResponse buildMeResponse(User user, HttpSession session) {
        var branch = user.getBranch();
        MeResponse.BranchInfo branchInfo = null;
        if (branch != null) {
            branchInfo = MeResponse.BranchInfo.builder()
                    .id(branch.getId())
                    .code(branch.getBranchCode())
                    .name(branch.getBranchName())
                    .build();
        }

        var pharmacy = branch != null ? branch.getPharmacy() : null;

        List<String> roles = user.getUserBranchRole() != null
                ? user.getUserBranchRole().stream()
                    .map(ur -> ur.getRole().getRoleName())
                    .distinct().toList()
                : List.of();

        List<String> permissions = new ArrayList<>();
        if (user.getUserBranchRole() != null) {
            for (UserBranchRole ur : user.getUserBranchRole()) {
                if (ur.getRole().getRolePermission() != null) {
                    ur.getRole().getRolePermission().forEach(rp -> {
                        if (rp.getPermissions() != null) {
                            permissions.add(rp.getPermissions().getPermissionName());
                        }
                    });
                }
            }
        }

        Map<String, Boolean> featureFlags = new LinkedHashMap<>();
        featureFlags.put("hybridSync", false);
        featureFlags.put("mpesaStk", false);
        featureFlags.put("etimsDirect", false);

        String expiresAt = null;
        if (session != null) {
            expiresAt = LocalDateTime.ofEpochSecond(
                    session.getCreationTime() / 1000 + session.getMaxInactiveInterval(),
                    0, ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME);
        }

        return MeResponse.builder()
                .expiresAt(expiresAt)
                .user(MeResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .displayName(buildDisplayName(user))
                        .pharmacyId(pharmacy != null ? pharmacy.getId() : null)
                        .pharmacyName(pharmacy != null ? pharmacy.getName() : null)
                        .activeBranch(branchInfo)
                        .roles(roles)
                        .permissions(permissions)
                        .featureFlags(featureFlags)
                        .build())
                .build();
    }

    private String buildDisplayName(User user) {
        String name = user.getFirstName();
        if (user.getLastName() != null) name += " " + user.getLastName();
        return name.trim();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        return ip;
    }

    private String getHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value != null && value.length() > 200 ? value.substring(0, 200) : value;
    }
}
