package com.example.gateway.config;

import com.example.gateway.security.CustomAuthorizationManager;
import com.example.gateway.security.EndpointSecurityScanner;
import org.springframework.context.ApplicationContext;
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
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * Centralized security configuration for all modules.
 * Security rules are automatically discovered from annotations on controller methods:
 * @RequiresReadDeclaration, @RequiresWriteDeclaration, @RequiresApproveDeclaration,
 * @RequiresReadWare, @RequiresWriteWare, @RequiresWareInventory, @RequiresGeneralAccess
 * 
 * No need to manually specify .pathMatchers() - all endpoints are automatically scanned
 * and configured based on their annotations.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    private final CustomAuthorizationManager authorizationManager;
    private final ApplicationContext applicationContext;

    public SecurityConfig(CustomAuthorizationManager authorizationManager, 
                         ApplicationContext applicationContext) {
        this.authorizationManager = authorizationManager;
        this.applicationContext = applicationContext;
    }

    @Bean
    public EndpointSecurityScanner endpointSecurityScanner() {
        return new EndpointSecurityScanner(applicationContext, authorizationManager);
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, 
                                                         EndpointSecurityScanner scanner) {
        // Automatically scan all endpoints and their security annotations
        Map<String, BiFunction<Authentication, AuthorizationContext, Mono<AuthorizationDecision>>> securityMap = 
            scanner.scanEndpoints();
        
        return http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for API endpoints
                .authorizeExchange(exchanges -> {
                    // Automatically configure security for all discovered endpoints
                    for (Map.Entry<String, BiFunction<Authentication, AuthorizationContext, Mono<AuthorizationDecision>>> entry : securityMap.entrySet()) {
                        exchanges.pathMatchers(entry.getKey())
                                .access(entry.getValue());
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
