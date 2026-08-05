package com.example.pos.security.auth;

import com.example.pos.common.filter.XRequestIdFilter;
import com.example.pos.sync.auth.TerminalAuthFilter;
import com.example.pos.terminal.auth.TerminalAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler handler = new CsrfTokenRequestAttributeHandler();
        handler.setCsrfRequestAttributeName("_csrf");

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> {
                if (csrfEnabled) {
                    csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(handler)
                        .ignoringRequestMatchers(
                            "/api/v1/auth/**",
                            "/actuator/**",
                            "/api/v1/payments/mpesa/callback",
                            "/api/v1/payments/paystack/callback",
                            "/api/v1/payments/stripe/callback",
                            "/api/v1/sync/**",
                            "/swagger-ui/**",
                            "/v3/api-docs/**");
                } else {
                    csrf.disable();
                }
            })
            .sessionManagement(sm -> sm
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false))
            .addFilterBefore(xRequestIdFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(terminalAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(terminalAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**",
                    "/actuator/health", "/actuator/info",
                    "/swagger-ui/**", "/v3/api-docs/**",
                    "/api/v1/payments/mpesa/callback",
                    "/api/v1/payments/paystack/callback",
                    "/api/v1/payments/stripe/callback",
                    "/api/v1/sync/health").permitAll()
                .requestMatchers("/api/v1/sync/push").hasAnyRole("TERMINAL", "OWNER", "PLATFORM_ADMIN")
                .requestMatchers("/api/v1/pharmacies/**").hasAnyRole("OWNER", "PLATFORM_ADMIN")
                .requestMatchers("/api/v1/branches/**").hasAnyRole("OWNER", "PLATFORM_ADMIN")
                .requestMatchers("/api/v1/users/**").hasAnyRole("OWNER", "PLATFORM_ADMIN")
                .requestMatchers("/api/v1/login-history/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "PLATFORM_ADMIN")
                .requestMatchers("/api/v1/roles/**").hasAnyRole("OWNER", "PLATFORM_ADMIN")
                .requestMatchers("/api/v1/permissions/**").hasAnyRole("OWNER", "PLATFORM_ADMIN")
                .requestMatchers("/api/v1/system-settings/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "PLATFORM_ADMIN")
                .requestMatchers("/api/v1/medicines/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "PHARMACIST", "STORE_KEEPER")
                .requestMatchers("/api/v1/batches/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/v1/stock/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/v1/stock-movements/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/v1/suppliers/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/v1/purchase-orders/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/v1/goods-received/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/v1/supplier-invoices/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/v1/supplier-payments/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/v1/sales/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER", "PHARMACIST")
                .requestMatchers("/api/v1/payments/**").hasAnyRole("OWNER", "CASHIER")
                .requestMatchers("/api/v1/sale-returns/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER")
                .requestMatchers("/api/v1/prescriptions/**").hasAnyRole("OWNER", "PHARMACIST")
                .requestMatchers("/api/v1/dispensary/**").hasAnyRole("OWNER", "PHARMACIST")
                .requestMatchers("/api/v1/expenses/**").hasAnyRole("OWNER", "BRANCH_MANAGER")
                .requestMatchers("/api/v1/expense-categories/**").hasAnyRole("OWNER", "BRANCH_MANAGER")
                .requestMatchers("/api/v1/cash-drawers/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER")
                .requestMatchers("/api/v1/shifts/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER")
                .requestMatchers("/api/v1/expiry-logs/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/v1/etims/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "FINANCE")
                .requestMatchers("/api/v1/compliance/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "FINANCE")
                .requestMatchers("/api/v1/controlled-drugs/**").hasAnyRole("OWNER", "PHARMACIST")
                .requestMatchers("/api/v1/categories/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/v1/dosage-forms/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/v1/units/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/v1/taxes/**").hasAnyRole("OWNER", "BRANCH_MANAGER")
                .requestMatchers("/api/v1/tax-categories/**").hasAnyRole("OWNER", "BRANCH_MANAGER")
                .requestMatchers("/api/v1/invoices/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER", "PHARMACIST")
                .requestMatchers("/api/v1/credit-notes/**", "/api/v1/debit-notes/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "FINANCE")
                .requestMatchers("/api/v1/manufacturers/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/v1/user-branch-roles/**").hasAnyRole("OWNER", "PLATFORM_ADMIN")
                .requestMatchers("/api/v1/audit-logs/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "PLATFORM_ADMIN")
                .requestMatchers("/api/v1/reports/**").hasAnyRole("OWNER", "BRANCH_MANAGER")
                .requestMatchers("/api/v1/customers/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER", "PHARMACIST")
                .requestMatchers("/api/v1/notifications/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER", "PHARMACIST", "STORE_KEEPER")
                .requestMatchers("/api/v1/price-history/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/v1/cash-transactions/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER")
                .requestMatchers("/api/v1/pos/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER", "PHARMACIST")
                .requestMatchers("/api/v1/receipts/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER", "PHARMACIST")
                .requestMatchers("/api/v1/hardware/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER")
                .requestMatchers("/api/v1/sync/terminals/**").hasAnyRole("OWNER", "PLATFORM_ADMIN")
                .requestMatchers("/api/v1/terminals/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER", "PHARMACIST", "PLATFORM_ADMIN")
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
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
