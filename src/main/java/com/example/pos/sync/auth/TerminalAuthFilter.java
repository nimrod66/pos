package com.example.pos.sync.auth;

import com.example.pos.sync.model.Terminal;
import com.example.pos.sync.model.TerminalRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class TerminalAuthFilter extends OncePerRequestFilter {

    private final TerminalRepository terminalRepository;

    public TerminalAuthFilter(TerminalRepository terminalRepository) {
        this.terminalRepository = terminalRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/sync/push");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String apiKey = request.getHeader("X-API-Key");

        if (apiKey == null || apiKey.isEmpty()) {
            response.setStatus(401);
            response.getWriter().write("{\"error\":\"Missing X-API-Key header\"}");
            return;
        }

        Terminal terminal = terminalRepository.findByApiKey(apiKey).orElse(null);

        if (terminal == null || !terminal.isActive()) {
            response.setStatus(401);
            response.getWriter().write("{\"error\":\"Invalid or inactive API key\"}");
            return;
        }

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        terminal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_TERMINAL")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        log.debug("Terminal {} authenticated via API key", terminal.getName());
        filterChain.doFilter(request, response);
    }
}
