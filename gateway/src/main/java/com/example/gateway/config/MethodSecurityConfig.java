package com.example.gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;

/**
 * Configuration for method-level security.
 * Enables @PreAuthorize, @PostAuthorize, @Secured annotations on reactive methods.
 * 
 * Custom security methods are available in SpEL expressions as @securityMethods.methodName()
 * through the SecurityExpressionMethods bean component.
 */
@Configuration
@EnableReactiveMethodSecurity(useAuthorizationManager = true)
public class MethodSecurityConfig {
    // Method security is enabled via @EnableReactiveMethodSecurity
    // Custom methods are available through @securityMethods bean in SpEL expressions
}
