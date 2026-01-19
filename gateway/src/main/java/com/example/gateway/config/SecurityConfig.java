package com.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Centralized security configuration for all modules.
 * 
 * Security is now configured using method-level security annotations (@PreAuthorize) 
 * on controller methods. This eliminates the need for EndpointSecurityScanner.
 * 
 * All endpoints require authentication by default. Access control is defined 
 * using @PreAuthorize annotations on controller methods.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for API endpoints
                .authorizeExchange(exchanges -> {
                    // Permit actuator endpoints and public paths
                    exchanges.pathMatchers("/actuator/**").permitAll();
                    
                    // All other requests require authentication
                    // Method-level security (@PreAuthorize) handles authorization
                    exchanges.anyExchange().authenticated();
                })
                .httpBasic(httpBasic -> {}) // Enable basic authentication
                .build();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
        UserDetails admin = User.withUsername("admin")
                .passwordEncoder(passwordEncoder()::encode)
                .password("admin123")
                .roles("ADMIN")
                .authorities("ADMIN", "READ_DECLARATION", "WRITE_DECLARATION", 
                            "APPROVE_DECLARATION", "READ_WARE", "WRITE_WARE", "MANAGE_INVENTORY")
                .build();

        UserDetails user1 = User.withUsername("user1")
                .passwordEncoder(passwordEncoder()::encode)
                .password("user123")
                .roles("USER")
                .authorities("READ_DECLARATION", "WRITE_DECLARATION")
                .build();

        UserDetails approver = User.withUsername("approver")
                .passwordEncoder(passwordEncoder()::encode)
                .password("approver123")
                .roles("APPROVER")
                .authorities("READ_DECLARATION", "APPROVE_DECLARATION")
                .build();

        UserDetails user2 = User.withUsername("user2")
                .passwordEncoder(passwordEncoder()::encode)
                .password("user123")
                .roles("USER")
                .authorities("READ_WARE", "WRITE_WARE")
                .build();

        UserDetails inventoryManager = User.withUsername("inventory")
                .passwordEncoder(passwordEncoder()::encode)
                .password("inventory123")
                .roles("INVENTORY")
                .authorities("READ_WARE", "MANAGE_INVENTORY")
                .build();

        return new MapReactiveUserDetailsService(admin, user1, approver, user2, inventoryManager);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
