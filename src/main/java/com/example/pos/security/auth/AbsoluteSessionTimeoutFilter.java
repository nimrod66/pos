package com.example.pos.security.auth;

import com.example.pos.common.dto.ErrorDetail;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

public class AbsoluteSessionTimeoutFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final Duration absoluteTimeout;

    public AbsoluteSessionTimeoutFilter(ObjectMapper objectMapper,
                                        @Value("${pos.security.absolute-session-timeout:12h}")
                                        Duration absoluteTimeout) {
        this.objectMapper = objectMapper;
        this.absoluteTimeout = absoluteTimeout;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        boolean expired = session != null
                && System.currentTimeMillis() - session.getCreationTime() >= absoluteTimeout.toMillis();

        if (!expired) {
            filterChain.doFilter(request, response);
            return;
        }

        session.invalidate();
        SecurityContextHolder.clearContext();

        if (request.getRequestURI().equals("/api/v1/auth/csrf")
                || request.getRequestURI().equals("/api/v1/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), ErrorDetail.builder()
                .status(HttpServletResponse.SC_UNAUTHORIZED)
                .error("Unauthorized")
                .message("The session has reached its maximum lifetime")
                .errorCode("SESSION_EXPIRED")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
