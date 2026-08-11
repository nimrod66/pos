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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final SecurityContextRepository securityContextRepository;
    private final CsrfTokenRepository csrfTokenRepository;
    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;
    private final Duration inactivityTimeout;
    private final Duration absoluteSessionTimeout;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       LoginHistoryRepository loginHistoryRepository,
                       SecurityContextRepository securityContextRepository,
                       CsrfTokenRepository csrfTokenRepository,
                       FindByIndexNameSessionRepository<? extends Session> sessionRepository,
                       @Value("${spring.session.timeout:30m}") Duration inactivityTimeout,
                       @Value("${pos.security.absolute-session-timeout:12h}") Duration absoluteSessionTimeout) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
        this.sessionRepository = sessionRepository;
        this.inactivityTimeout = inactivityTimeout;
        this.absoluteSessionTimeout = absoluteSessionTimeout;
    }

    public MeResponse login(String email, String password, HttpServletRequest request,
                            HttpServletResponse response) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, password));

        revokePrincipalSessions(normalizedEmail);

        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setMaxInactiveInterval(Math.toIntExact(inactivityTimeout.toSeconds()));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        csrfTokenRepository.saveToken(null, request, response);

        User user = userRepository.findByEmail(normalizedEmail)
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
    public MeResponse me(UUID userId, HttpServletRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Authenticated user no longer exists"));
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
        userRepository.findById(userId).ifPresent(user -> revokePrincipalSessions(user.getEmail()));
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
                    .filter(ur -> branch != null && ur.getBranch() != null
                            && branch.getId().equals(ur.getBranch().getId()))
                    .map(ur -> ur.getRole().getRoleName())
                    .filter(Objects::nonNull)
                    .distinct().sorted().toList()
                : List.of();

        Set<String> permissions = new TreeSet<>();
        if (user.getUserBranchRole() != null) {
            for (UserBranchRole ur : user.getUserBranchRole()) {
                if (branch == null || ur.getBranch() == null
                        || !branch.getId().equals(ur.getBranch().getId())) {
                    continue;
                }
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
            Instant idleExpiry = Instant.ofEpochMilli(session.getLastAccessedTime())
                    .plusSeconds(session.getMaxInactiveInterval());
            Instant absoluteExpiry = Instant.ofEpochMilli(session.getCreationTime())
                    .plus(absoluteSessionTimeout);
            expiresAt = (idleExpiry.isBefore(absoluteExpiry) ? idleExpiry : absoluteExpiry).toString();
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
                        .permissions(List.copyOf(permissions))
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

    private void revokePrincipalSessions(String principalName) {
        sessionRepository.findByPrincipalName(principalName)
                .keySet()
                .forEach(sessionRepository::deleteById);
    }
}
