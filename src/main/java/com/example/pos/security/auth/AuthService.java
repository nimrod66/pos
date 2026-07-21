package com.example.pos.security.auth;

import com.example.pos.user.loginhistory.model.LoginHistory;
import com.example.pos.user.loginhistory.repository.LoginHistoryRepository;
import com.example.pos.user.users.model.User;
import com.example.pos.user.users.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Optional;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final LoginHistoryRepository loginHistoryRepository;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil,
                       PasswordEncoder passwordEncoder, LoginHistoryRepository loginHistoryRepository) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.loginHistoryRepository = loginHistoryRepository;
    }

    public LoginResult login(String email, String password, HttpServletRequest request, HttpServletResponse response) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (user.getStatus() != User.Status.ACTIVE) {
            throw new BadCredentialsException("Account is not active");
        }

        Long branchId = user.getBranch() != null ? user.getBranch().getId() : null;
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), branchId);
        String csrfToken = java.util.UUID.randomUUID().toString();

        setJwtCookie(response, token);
        response.setHeader("X-CSRF-TOKEN", csrfToken);

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

        return new LoginResult(user.getId(), user.getEmail(), user.getFirstName(),
                branchId, csrfToken, 86400000);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String token = extractToken(request);
        if (token != null && jwtUtil.validateToken(token)) {
            String email = jwtUtil.getEmail(token);
            Optional<User> user = userRepository.findByEmail(email);
            user.ifPresent(u -> {
                loginHistoryRepository.findTopByUserIdOrderByLoginTimeDesc(u.getId())
                        .ifPresent(h -> {
                            h.setLogoutTime(LocalDateTime.now());
                            loginHistoryRepository.save(h);
                        });
            });
        }
        Cookie clear = new Cookie("jwt_token", null);
        clear.setHttpOnly(true);
        clear.setPath("/");
        clear.setMaxAge(0);
        response.addCookie(clear);
    }

    public String refresh(HttpServletRequest request, HttpServletResponse response) {
        String token = extractToken(request);
        if (token != null && jwtUtil.validateToken(token)) {
            String email = jwtUtil.getEmail(token);
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                Long branchId = user.getBranch() != null ? user.getBranch().getId() : null;
                String newToken = jwtUtil.generateToken(user.getId(), email, branchId);
                setJwtCookie(response, newToken);
                return newToken;
            }
        }
        throw new BadCredentialsException("Invalid or expired token");
    }

    private void setJwtCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("jwt_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(86400);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private String extractToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(c -> "jwt_token".equals(c.getName()))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);
        }
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
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
