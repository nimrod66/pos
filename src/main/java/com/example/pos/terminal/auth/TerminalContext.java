package com.example.pos.terminal.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

public final class TerminalContext {

    private TerminalContext() {
    }

    public static Optional<TerminalPrincipal> getCurrentTerminal() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return Optional.empty();
        }
        HttpServletRequest request = attributes.getRequest();
        TerminalPrincipal principal = (TerminalPrincipal) request.getAttribute("terminalPrincipal");
        return Optional.ofNullable(principal);
    }

    public static String getCurrentTerminalId() {
        return getCurrentTerminal()
                .map(TerminalPrincipal::getTerminalId)
                .orElse(null);
    }
}
