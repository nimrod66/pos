package com.example.pos.security.auth;

import com.example.pos.common.filter.XRequestIdFilter;
import com.example.pos.common.dto.ErrorDetail;
import com.example.pos.sync.auth.TerminalAuthFilter;
import com.example.pos.terminal.auth.TerminalAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final TerminalAuthFilter terminalAuthFilter;
    private final TerminalAuthenticationFilter terminalAuthenticationFilter;
    private final XRequestIdFilter xRequestIdFilter;

    @Value("${pos.security.csrf-enabled:true}")
    private boolean csrfEnabled;

    @Value("${pos.security.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(TerminalAuthFilter terminalAuthFilter,
                          TerminalAuthenticationFilter terminalAuthenticationFilter,
                          XRequestIdFilter xRequestIdFilter) {
        this.terminalAuthFilter = terminalAuthFilter;
        this.terminalAuthenticationFilter = terminalAuthenticationFilter;
        this.xRequestIdFilter = xRequestIdFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public HttpSessionCsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
        repository.setHeaderName("X-XSRF-TOKEN");
        return repository;
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public AbsoluteSessionTimeoutFilter absoluteSessionTimeoutFilter(ObjectMapper objectMapper,
            @Value("${pos.security.absolute-session-timeout:12h}") java.time.Duration timeout) {
        return new AbsoluteSessionTimeoutFilter(objectMapper, timeout);
    }

    @Bean
    public FilterRegistrationBean<AbsoluteSessionTimeoutFilter> absoluteSessionFilterRegistration(
            AbsoluteSessionTimeoutFilter filter) {
        FilterRegistrationBean<AbsoluteSessionTimeoutFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           HttpSessionCsrfTokenRepository csrfTokenRepository,
                                           SecurityContextRepository securityContextRepository,
                                           ObjectMapper objectMapper,
                                           AbsoluteSessionTimeoutFilter absoluteSessionTimeoutFilter) throws Exception {
        CsrfTokenRequestAttributeHandler handler = new CsrfTokenRequestAttributeHandler();
        handler.setCsrfRequestAttributeName("_csrf");

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .securityContext(context -> context
                .securityContextRepository(securityContextRepository)
                .requireExplicitSave(true))
            .csrf(csrf -> {
                if (csrfEnabled) {
                    csrf.csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(handler)
                        .ignoringRequestMatchers(
                            "/api/v1/payments/mpesa/callback",
                            "/api/v1/payments/paystack/callback",
                            "/api/v1/payments/stripe/callback",
                            "/api/v1/sync/**");
                } else {
                    csrf.disable();
                }
            })
            .sessionManagement(sm -> sm
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) ->
                    writeSecurityError(objectMapper, response, request.getRequestURI(),
                        HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHENTICATED",
                        "Authentication is required"))
                .accessDeniedHandler((request, response, exception) -> {
                    String code = exception instanceof MissingCsrfTokenException
                            || exception instanceof InvalidCsrfTokenException
                            ? "CSRF_VALIDATION_FAILED" : "ACCESS_DENIED";
                    String message = "CSRF_VALIDATION_FAILED".equals(code)
                            ? "The secure request token is missing or invalid"
                            : "You do not have permission to perform this action";
                    writeSecurityError(objectMapper, response, request.getRequestURI(),
                        HttpServletResponse.SC_FORBIDDEN, code, message);
                }))
            .addFilterBefore(xRequestIdFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(absoluteSessionTimeoutFilter, SecurityContextHolderFilter.class)
            .addFilterBefore(terminalAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(terminalAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/csrf", "/api/v1/auth/login",
                    "/actuator/health", "/actuator/info",
                    "/api/v1/payments/mpesa/callback",
                    "/api/v1/payments/paystack/callback",
                    "/api/v1/payments/stripe/callback",
                    "/api/v1/sync/health").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").authenticated()
                .requestMatchers("/api/v1/auth/me", "/api/v1/auth/logout").authenticated()
                // Deferred modules stay unreachable until their tenant and accounting workflows are complete.
                .requestMatchers(
                    "/api/v1/insurance/**",
                    "/api/v1/etims/**",
                    "/api/v1/compliance/**",
                    "/api/v1/invoices/**",
                    "/api/v1/credit-notes/**",
                    "/api/v1/debit-notes/**",
                    "/api/v1/receipts/fiscal/**",
                    "/api/v1/expenses/**",
                    "/api/v1/expense-categories/**",
                    "/api/v1/supplier-invoices/**",
                    "/api/v1/supplier-payments/**").denyAll()
                // Shared reference dictionaries are read-only in the pharmacy-facing MVP.
                .requestMatchers(HttpMethod.POST,
                    "/api/v1/categories/**", "/api/v1/dosage-forms/**",
                    "/api/v1/units/**", "/api/v1/taxes/**",
                    "/api/v1/tax-categories/**", "/api/v1/manufacturers/**",
                    "/api/v1/catalog/**").denyAll()
                .requestMatchers(HttpMethod.PUT,
                    "/api/v1/categories/**", "/api/v1/dosage-forms/**",
                    "/api/v1/units/**", "/api/v1/taxes/**",
                    "/api/v1/tax-categories/**", "/api/v1/manufacturers/**",
                    "/api/v1/catalog/**").denyAll()
                .requestMatchers(HttpMethod.PATCH,
                    "/api/v1/categories/**", "/api/v1/dosage-forms/**",
                    "/api/v1/units/**", "/api/v1/taxes/**",
                    "/api/v1/tax-categories/**", "/api/v1/manufacturers/**",
                    "/api/v1/catalog/**").denyAll()
                .requestMatchers(HttpMethod.DELETE,
                    "/api/v1/categories/**", "/api/v1/dosage-forms/**",
                    "/api/v1/units/**", "/api/v1/taxes/**",
                    "/api/v1/tax-categories/**", "/api/v1/manufacturers/**",
                    "/api/v1/catalog/**").denyAll()
                .requestMatchers("/api/v1/sync/push").hasAnyRole("TERMINAL", "OWNER", "PLATFORM_ADMIN")
                .requestMatchers("/api/v1/pharmacies/**").hasAnyRole("OWNER", "PLATFORM_ADMIN")
                .requestMatchers("/api/v1/branches/**").hasAnyRole("OWNER", "PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/users/*/password").authenticated()
                .requestMatchers("/api/v1/users/**").hasAnyRole("OWNER", "PLATFORM_ADMIN")
                .requestMatchers("/api/v1/login-history/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "PLATFORM_ADMIN")
                .requestMatchers("/api/v1/roles/**").hasAnyRole("OWNER", "PLATFORM_ADMIN")
                .requestMatchers("/api/v1/permissions/**").hasAnyRole("OWNER", "PLATFORM_ADMIN")
                
                .requestMatchers("/api/v1/system-settings/**").authenticated()
                .requestMatchers("/api/v1/medicines/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "PHARMACIST", "CASHIER", "STORE_KEEPER", "PHARMACY_TECHNICIAN")
                .requestMatchers("/api/v1/batches/**").hasAnyAuthority(
                    PermissionCodes.INVENTORY_READ, PermissionCodes.INVENTORY_ADJUST_APPROVE)
                .requestMatchers("/api/v1/stock/**").hasAnyAuthority(
                    PermissionCodes.INVENTORY_READ, PermissionCodes.INVENTORY_ADJUST_APPROVE)
                .requestMatchers("/api/v1/stock-movements/**").hasAnyAuthority(
                    PermissionCodes.INVENTORY_READ,
                    PermissionCodes.INVENTORY_ADJUST_REQUEST,
                    PermissionCodes.INVENTORY_ADJUST_APPROVE)
                .requestMatchers("/api/v1/suppliers/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER", "PHARMACY_TECHNICIAN")
                .requestMatchers("/api/v1/purchase-orders/**").hasAnyAuthority(
                    PermissionCodes.PURCHASE_ORDER_READ,
                    PermissionCodes.PURCHASE_ORDER_WRITE,
                    PermissionCodes.INVENTORY_ADJUST_APPROVE,
                    PermissionCodes.INVENTORY_RECEIVE)
                .requestMatchers("/api/v1/goods-received/**").hasAnyAuthority(
                    PermissionCodes.INVENTORY_READ, PermissionCodes.INVENTORY_RECEIVE)
                .requestMatchers("/api/v1/supplier-invoices/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/v1/supplier-payments/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/v1/sales/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER", "PHARMACIST", "PHARMACY_TECHNICIAN")
                .requestMatchers("/api/v1/payments/**").hasAnyRole("OWNER", "CASHIER", "PHARMACIST", "PHARMACY_TECHNICIAN")
                .requestMatchers("/api/v1/sale-returns/**").hasAnyAuthority(
                    PermissionCodes.SALE_READ, PermissionCodes.SALE_RETURN)
                .requestMatchers("/api/v1/prescriptions/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "PHARMACIST", "PHARMACY_TECHNICIAN")
                .requestMatchers("/api/v1/dispensary/**").hasAnyRole("OWNER", "PHARMACIST")
                .requestMatchers("/api/v1/expenses/**").hasAnyRole("OWNER", "BRANCH_MANAGER")
                .requestMatchers("/api/v1/expense-categories/**").hasAnyRole("OWNER", "BRANCH_MANAGER")
                .requestMatchers("/api/v1/cash-drawers/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER")
                .requestMatchers("/api/v1/shifts/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "PHARMACIST", "CASHIER", "PHARMACY_TECHNICIAN")
                .requestMatchers("/api/v1/expiry-logs/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/v1/etims/**").hasAnyRole("OWNER", "BRANCH_MANAGER")
                .requestMatchers("/api/v1/compliance/**").hasAnyRole("OWNER", "BRANCH_MANAGER")
                .requestMatchers("/api/v1/controlled-drugs/**").hasAnyRole("OWNER", "PHARMACIST")
                .requestMatchers("/api/v1/categories/**").hasAuthority(PermissionCodes.MEDICINE_READ)
                .requestMatchers("/api/v1/dosage-forms/**").hasAuthority(PermissionCodes.MEDICINE_READ)
                .requestMatchers("/api/v1/units/**").hasAuthority(PermissionCodes.MEDICINE_READ)
                .requestMatchers("/api/v1/taxes/**").hasAnyRole("OWNER", "BRANCH_MANAGER")
                .requestMatchers("/api/v1/tax-categories/**").hasAuthority(PermissionCodes.MEDICINE_READ)
                .requestMatchers("/api/v1/invoices/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER", "PHARMACIST")
                .requestMatchers("/api/v1/credit-notes/**", "/api/v1/debit-notes/**").hasAnyRole("OWNER", "BRANCH_MANAGER")
                .requestMatchers("/api/v1/manufacturers/**").hasAuthority(PermissionCodes.MEDICINE_READ)
                .requestMatchers("/api/v1/user-branch-roles/**").hasAnyRole("OWNER", "PLATFORM_ADMIN")
                .requestMatchers("/api/v1/audit-logs/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "PLATFORM_ADMIN")
                .requestMatchers("/api/v1/reports/**").hasAnyAuthority(
                    PermissionCodes.DASHBOARD_READ,
                    PermissionCodes.REPORT_SALES_READ,
                    PermissionCodes.REPORT_INVENTORY_READ)
                .requestMatchers("/api/v1/customers/**").hasAnyAuthority(
                    PermissionCodes.CUSTOMER_READ,
                    PermissionCodes.CUSTOMER_WRITE,
                    PermissionCodes.SETTINGS_MANAGE)
                .requestMatchers("/api/v1/notifications/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER", "PHARMACIST", "STORE_KEEPER", "PHARMACY_TECHNICIAN")
                .requestMatchers("/api/v1/price-history/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/v1/cash-transactions/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER")
                .requestMatchers("/api/v1/pos/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER", "PHARMACIST", "PHARMACY_TECHNICIAN")
                .requestMatchers("/api/v1/receipts/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER", "PHARMACIST", "PHARMACY_TECHNICIAN")
                .requestMatchers("/api/v1/hardware/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER", "PHARMACIST", "STORE_KEEPER", "PHARMACY_TECHNICIAN")
                .requestMatchers("/api/v1/sync/terminals/**").hasAnyRole("OWNER", "PLATFORM_ADMIN")
                .requestMatchers("/api/v1/terminals/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER", "PHARMACIST", "STORE_KEEPER", "PHARMACY_TECHNICIAN", "PLATFORM_ADMIN")
                .requestMatchers("/api/v1/sync/**").authenticated()
                .requestMatchers("/api/v1/system/**").authenticated()
                .requestMatchers("/api/v1/catalog/**").authenticated()
                .requestMatchers("/api/v1/insurance/**").authenticated()
                .anyRequest().authenticated());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Accept", "Content-Type", "Idempotency-Key",
                "X-CSRF-TOKEN", "X-XSRF-TOKEN", "X-Request-ID"));
        config.setExposedHeaders(List.of("X-Request-ID"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private static void writeSecurityError(ObjectMapper objectMapper,
                                           HttpServletResponse response,
                                           String path,
                                           int status,
                                           String errorCode,
                                           String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        ErrorDetail body = ErrorDetail.builder()
                .status(status)
                .error(status == 401 ? "Unauthorized" : "Forbidden")
                .message(message)
                .errorCode(errorCode)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
