package com.example.gateway.config;

import com.example.gateway.security.AuthorizationRule;
import com.example.gateway.security.CustomAuthorizationManager;
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

import java.util.List;

/**
 * Centralized security configuration for all modules
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    private final CustomAuthorizationManager authorizationManager;
    private final List<AuthorizationRule> authorizationRules;

    public SecurityConfig(CustomAuthorizationManager authorizationManager, 
                         List<AuthorizationRule> authorizationRules) {
        this.authorizationManager = authorizationManager;
        this.authorizationRules = authorizationRules;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for API endpoints
                .authorizeExchange(exchanges -> {
                    // Apply authorization rules from configuration
                    for (AuthorizationRule rule : authorizationRules) {
                        for (String pathPattern : rule.getPathPatterns()) {
                            exchanges.pathMatchers(pathPattern)
                                    .access(rule.getAuthorizationMethod());
                        }
                    }
                    
                    // All other requests require authentication
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

    @Bean
    public CustomAuthorizationManager authorizationManager() {
        return new CustomAuthorizationManager();
    }
}
