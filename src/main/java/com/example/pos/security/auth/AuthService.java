package com.example.pos.security.auth;

import com.example.pos.user.loginhistory.model.LoginHistory;
import com.example.pos.user.loginhistory.repository.LoginHistoryRepository;
import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ForbiddenException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.operations.model.OperationalMetricEvent;
import com.example.pos.operations.service.OperationalMetricsService;
import com.example.pos.sale.payment.service.MpesaSettings;
import com.example.pos.core.branch.repository.BranchRepository;
import com.example.pos.user.staffshifts.model.StaffShifts;
import com.example.pos.user.staffshifts.repository.StaffShiftsRepository;
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
import org.springframework.security.core.userdetails.UserDetails;
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
    private final BranchRepository branchRepository;
    private final StaffShiftsRepository staffShiftsRepository;
    private final UserDetailsServiceImpl userDetailsService;
    private final SecurityContextRepository securityContextRepository;
    private final CsrfTokenRepository csrfTokenRepository;
    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;
    private final Duration inactivityTimeout;
    private final Duration absoluteSessionTimeout;
    private final MpesaSettings mpesaSettings;
    private final LoginLockoutService lockoutService;
    private final AuthFailedLoginRepository failedLoginRepository;
    private final OperationalMetricsService metricsService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       LoginHistoryRepository loginHistoryRepository,
                       BranchRepository branchRepository,
                       StaffShiftsRepository staffShiftsRepository,
                       UserDetailsServiceImpl userDetailsService,
                       SecurityContextRepository securityContextRepository,
                       CsrfTokenRepository csrfTokenRepository,
                       FindByIndexNameSessionRepository<? extends Session> sessionRepository,
                       @Value("${spring.session.timeout:30m}") Duration inactivityTimeout,
                       @Value("${pos.security.absolute-session-timeout:12h}") Duration absoluteSessionTimeout,
                       MpesaSettings mpesaSettings,
                       LoginLockoutService lockoutService,
                       AuthFailedLoginRepository failedLoginRepository,
                       OperationalMetricsService metricsService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.branchRepository = branchRepository;
        this.staffShiftsRepository = staffShiftsRepository;
        this.userDetailsService = userDetailsService;
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
        this.sessionRepository = sessionRepository;
        this.inactivityTimeout = inactivityTimeout;
        this.absoluteSessionTimeout = absoluteSessionTimeout;
        this.mpesaSettings = mpesaSettings;
        this.lockoutService = lockoutService;
        this.failedLoginRepository = failedLoginRepository;
        this.metricsService = metricsService;
    }

    private static final int MAX_FAILED_LOGINS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    public MeResponse login(String email, String password, HttpServletRequest request,
                            HttpServletResponse response) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        enforceLockout(normalizedEmail);
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, password));
            lockoutService.clearFailures(normalizedEmail);
            metricsService.record(OperationalMetricEvent.EventType.LOGIN,
                    OperationalMetricEvent.EventStatus.SUCCESS, "LOGIN_SUCCESS", "auth-service",
                    null, null, null, null, normalizedEmail);
        } catch (BadCredentialsException ex) {
            recordFailedLogin(normalizedEmail);
            metricsService.record(OperationalMetricEvent.EventType.LOGIN,
                    OperationalMetricEvent.EventStatus.FAILED, "BAD_CREDENTIALS", "auth-service",
                    null, null, null, null, normalizedEmail);
            throw ex;
        }

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

    private void enforceLockout(String normalizedEmail) {
        lockoutService.enforceLockout(normalizedEmail);
    }

    private void recordFailedLogin(String normalizedEmail) {
        lockoutService.recordFailure(normalizedEmail);
    }

    public MeResponse switchBranch(UUID userId, UUID branchId,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        User user = userRepository.findContextById(userId)
                .orElseThrow(() -> new BadCredentialsException(
                        "Authenticated user no longer exists"));
        Branch activeBranch = user.getBranch();
        if (activeBranch == null || activeBranch.getPharmacy() == null) {
            throw new ForbiddenException("The account has no pharmacy context");
        }
        Branch target = branchRepository.findByIdAndPharmacyId(
                        branchId, activeBranch.getPharmacy().getId())
                .filter(branch -> branch.getStatus() == Branch.Status.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active branch", branchId));
        boolean pharmacyOwner = user.getUserBranchRole().stream().anyMatch(assignment ->
                assignment.getRole() != null
                        && "OWNER".equals(assignment.getRole().getRoleName())
                        && assignment.getBranch() != null
                        && assignment.getBranch().getPharmacy() != null
                        && activeBranch.getPharmacy().getId().equals(
                                assignment.getBranch().getPharmacy().getId()));
        if (!pharmacyOwner) {
            throw new ForbiddenException("Only a pharmacy owner can switch branches");
        }
        if (activeBranch.getId().equals(target.getId())) {
            return buildMeResponse(user, request.getSession(false));
        }
        if (staffShiftsRepository.existsByUserIdAndStatus(
                userId, StaffShifts.Status.ACTIVE)) {
            throw new ConflictException(
                    "Close the current shift before switching branches",
                    "OPEN_SHIFT_BLOCKS_BRANCH_SWITCH");
        }

        user.setBranch(target);
        userRepository.saveAndFlush(user);

        UserDetails details = userDetailsService.loadUserByUsername(user.getEmail());
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                details, null, details.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.changeSessionId();
        securityContextRepository.saveContext(context, request, response);
        csrfTokenRepository.saveToken(null, request, response);
        return buildMeResponse(user, request.getSession(false));
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
                    .filter(ur -> roleAppliesToBranch(ur, branch))
                    .map(ur -> ur.getRole().getRoleName())
                    .filter(Objects::nonNull)
                    .distinct().sorted().toList()
                : List.of();

        Set<String> permissions = new TreeSet<>();
        if (user.getUserBranchRole() != null) {
            for (UserBranchRole ur : user.getUserBranchRole()) {
                if (!roleAppliesToBranch(ur, branch)) continue;
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
        featureFlags.put("mpesaStk", mpesaSettings.resolve().stkReady());
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

    private boolean roleAppliesToBranch(UserBranchRole assignment, Branch activeBranch) {
        if (activeBranch == null || assignment.getBranch() == null
                || assignment.getRole() == null) {
            return false;
        }
        if (activeBranch.getId().equals(assignment.getBranch().getId())) {
            return true;
        }
        return "OWNER".equals(assignment.getRole().getRoleName())
                && activeBranch.getPharmacy() != null
                && assignment.getBranch().getPharmacy() != null
                && activeBranch.getPharmacy().getId().equals(
                        assignment.getBranch().getPharmacy().getId());
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
