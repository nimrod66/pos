package com.example.pos.security.auth;

import com.example.pos.sync.auth.TerminalAuthFilter;
import com.example.pos.terminal.auth.TerminalAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    private final JwtAuthenticationFilter jwtFilter;
    private final TerminalAuthFilter terminalAuthFilter;
    private final TerminalAuthenticationFilter terminalAuthenticationFilter;

    @Value("${pos.security.csrf-enabled:true}")
    private boolean csrfEnabled;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter,
                          TerminalAuthFilter terminalAuthFilter,
                          TerminalAuthenticationFilter terminalAuthenticationFilter) {
        this.jwtFilter = jwtFilter;
        this.terminalAuthFilter = terminalAuthFilter;
        this.terminalAuthenticationFilter = terminalAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
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
                        .ignoringRequestMatchers("/api/auth/**", "/actuator/**", "/api/payments/mpesa/callback", "/api/payments/paystack/callback", "/api/payments/stripe/callback", "/api/sync/**");
                } else {
                    csrf.disable();
                }
            })
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(terminalAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(terminalAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/actuator/health", "/actuator/info", "/swagger-ui/**", "/v3/api-docs/**", "/api/payments/mpesa/callback", "/api/payments/paystack/callback", "/api/payments/stripe/callback", "/api/sync/health").permitAll()
                .requestMatchers("/api/sync/push").hasAnyRole("TERMINAL", "OWNER", "PLATFORM_ADMIN")
                .requestMatchers("/api/pharmacies/**").hasAnyRole("OWNER", "PLATFORM_ADMIN")
                .requestMatchers("/api/branches/**").hasAnyRole("OWNER", "PLATFORM_ADMIN")
                .requestMatchers("/api/users/**").hasAnyRole("OWNER", "PLATFORM_ADMIN")
                .requestMatchers("/api/login-history/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "PLATFORM_ADMIN")
                .requestMatchers("/api/roles/**").hasAnyRole("OWNER", "PLATFORM_ADMIN")
                .requestMatchers("/api/permissions/**").hasAnyRole("OWNER", "PLATFORM_ADMIN")
                .requestMatchers("/api/system-settings/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "PLATFORM_ADMIN")
                .requestMatchers("/api/medicines/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "PHARMACIST", "STORE_KEEPER")
                .requestMatchers("/api/batches/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/stock/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/stock-movements/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/suppliers/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/purchase-orders/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/goods-received/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/supplier-invoices/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/supplier-payments/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/sales/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER", "PHARMACIST")
                .requestMatchers("/api/payments/**").hasAnyRole("OWNER", "CASHIER")
                .requestMatchers("/api/sale-returns/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER")
                .requestMatchers("/api/prescriptions/**").hasAnyRole("OWNER", "PHARMACIST")
                .requestMatchers("/api/dispensary/**").hasAnyRole("OWNER", "PHARMACIST")
                .requestMatchers("/api/expenses/**").hasAnyRole("OWNER", "BRANCH_MANAGER")
                .requestMatchers("/api/expense-categories/**").hasAnyRole("OWNER", "BRANCH_MANAGER")
                .requestMatchers("/api/cash-drawers/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER")
                .requestMatchers("/api/shifts/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER")
                .requestMatchers("/api/expiry-logs/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/etims/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "FINANCE")
                .requestMatchers("/api/compliance/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "FINANCE")
                .requestMatchers("/api/controlled-drugs/**").hasAnyRole("OWNER", "PHARMACIST")
                .requestMatchers("/api/categories/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/dosage-forms/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/units/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/taxes/**").hasAnyRole("OWNER", "BRANCH_MANAGER")
                .requestMatchers("/api/tax-categories/**").hasAnyRole("OWNER", "BRANCH_MANAGER")
                .requestMatchers("/api/invoices/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER", "PHARMACIST")
                .requestMatchers("/api/credit-notes/**", "/api/debit-notes/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "FINANCE")
                .requestMatchers("/api/manufacturers/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/user-branch-roles/**").hasAnyRole("OWNER", "PLATFORM_ADMIN")
                .requestMatchers("/api/audit-logs/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "PLATFORM_ADMIN")
                .requestMatchers("/api/reports/**").hasAnyRole("OWNER", "BRANCH_MANAGER")
                .requestMatchers("/api/customers/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER", "PHARMACIST")
                .requestMatchers("/api/notifications/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER", "PHARMACIST", "STORE_KEEPER")
                .requestMatchers("/api/price-history/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "STORE_KEEPER")
                .requestMatchers("/api/cash-transactions/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER")
                .requestMatchers("/api/pos/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER", "PHARMACIST")
                .requestMatchers("/api/receipts/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER", "PHARMACIST")
                .requestMatchers("/api/hardware/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER")
                .requestMatchers("/api/sync/terminals/**").hasAnyRole("OWNER", "PLATFORM_ADMIN")
                .requestMatchers("/api/terminals/**").hasAnyRole("OWNER", "BRANCH_MANAGER", "CASHIER", "PHARMACIST", "PLATFORM_ADMIN")
                .requestMatchers("/api/sync/**").authenticated()
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
