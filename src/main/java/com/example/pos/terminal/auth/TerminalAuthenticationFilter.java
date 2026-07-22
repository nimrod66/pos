package com.example.pos.terminal.auth;

import com.example.pos.terminal.model.Terminal;
import com.example.pos.terminal.repository.TerminalRegistryRepository;
import com.example.pos.terminal.model.TerminalStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class TerminalAuthenticationFilter extends OncePerRequestFilter {

    private final TerminalRegistryRepository terminalRepository;

    public TerminalAuthenticationFilter(TerminalRegistryRepository terminalRepository) {
        this.terminalRepository = terminalRepository;
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
}
