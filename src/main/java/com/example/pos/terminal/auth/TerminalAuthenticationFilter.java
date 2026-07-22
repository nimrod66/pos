package com.example.pos.terminal.auth;

import com.example.pos.terminal.model.Terminal;
import com.example.pos.terminal.model.TerminalStatus;
import com.example.pos.terminal.repository.TerminalRegistryRepository;
import com.example.pos.terminal.service.TerminalRegistrationService;
import com.example.pos.terminal.service.TerminalSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
public class TerminalAuthenticationFilter extends OncePerRequestFilter {

    private final TerminalRegistryRepository terminalRepository;
    private final TerminalSessionService sessionService;
    private final TerminalRegistrationService registrationService;

    public TerminalAuthenticationFilter(TerminalRegistryRepository terminalRepository,
                                         TerminalSessionService sessionService,
                                         TerminalRegistrationService registrationService) {
        this.terminalRepository = terminalRepository;
        this.sessionService = sessionService;
        this.registrationService = registrationService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String terminalId = request.getHeader("X-Terminal-ID");
        String terminalVersion = request.getHeader("X-Terminal-Version");
        String terminalType = request.getHeader("X-Terminal-Type");

        if (terminalId == null || terminalId.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        terminalRepository.findByTerminalId(terminalId).ifPresentOrElse(
                terminal -> {
                    if (terminal.getStatus() != TerminalStatus.ACTIVE) {
                        log.warn("Terminal {} is not active (status: {})", terminalId, terminal.getStatus());
                        try {
                            response.setStatus(403);
                            response.getWriter().write("{\"error\":\"Terminal is not active\"}");
                            return;
                        } catch (IOException e) {
                            log.error("Error writing terminal inactive response", e);
                        }
                    }

                    if (!validateVersion(terminal, terminalVersion, response)) {
                        return;
                    }

                    TerminalPrincipal principal = TerminalPrincipal.builder()
                            .terminalId(terminal.getTerminalId())
                            .name(terminal.getName())
                            .terminalType(terminal.getTerminalType())
                            .appVersion(terminalVersion)
                            .branchId(terminal.getBranchId())
                            .active(true)
                            .build();

                    request.setAttribute("terminalPrincipal", principal);
                    request.setAttribute("terminal", terminal);

                    registrationService.updateLastSeen(terminalId);
                    autoCreateSessionIfNeeded(terminal, request);

                    log.debug("Terminal {} authenticated via X-Terminal-ID header", terminalId);
                },
                () -> {
                    log.warn("Unknown terminal ID: {}", terminalId);
                    try {
                        response.setStatus(401);
                        response.getWriter().write("{\"error\":\"Unknown terminal ID\"}");
                        return;
                    } catch (IOException e) {
                        log.error("Error writing unknown terminal response", e);
                    }
                }
        );

        if (response.getStatus() >= 400) {
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean validateVersion(Terminal terminal, String terminalVersion, HttpServletResponse response) {
        if (terminal.getMinimumBackendVersion() == null || terminal.getMinimumBackendVersion().isEmpty()) {
            return true;
        }
        if (terminalVersion == null || terminalVersion.isEmpty()) {
            log.warn("Terminal {} missing X-Terminal-Version header, min backend version required: {}",
                    terminal.getTerminalId(), terminal.getMinimumBackendVersion());
            try {
                response.setStatus(426);
                response.getWriter().write(
                        "{\"error\":\"Terminal version required. Minimum backend: " +
                        terminal.getMinimumBackendVersion() + "\"}");
            } catch (IOException e) {
                log.error("Error writing version mismatch response", e);
            }
            return false;
        }

        if (compareVersions(terminalVersion, terminal.getMinimumBackendVersion()) < 0) {
            log.warn("Terminal {} version {} is below minimum backend version {}",
                    terminal.getTerminalId(), terminalVersion, terminal.getMinimumBackendVersion());
            try {
                response.setStatus(426);
                response.getWriter().write(
                        "{\"error\":\"Terminal version " + terminalVersion +
                        " is outdated. Minimum required: " + terminal.getMinimumBackendVersion() + "\"}");
            } catch (IOException e) {
                log.error("Error writing outdated version response", e);
            }
            return false;
        }

        return true;
    }

    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int len = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < len; i++) {
            int p1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int p2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
            if (p1 != p2) return Integer.compare(p1, p2);
        }
        return 0;
    }

    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void autoCreateSessionIfNeeded(Terminal terminal, HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        String token = authHeader.substring(7);
        if (!sessionService.validateToken(token)) {
            sessionService.createSession(
                    terminal, token,
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent"),
                    null,
                    TerminalSessionService.DEFAULT_SESSION_TIMEOUT_MINUTES
            );
        } else {
            sessionService.refreshByToken(token);
        }
    }
}
