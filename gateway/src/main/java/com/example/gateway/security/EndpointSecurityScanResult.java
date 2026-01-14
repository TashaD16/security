package com.example.gateway.security;

import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.web.server.authorization.AuthorizationContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Result of endpoint security scanning
 */
public class EndpointSecurityScanResult {
    
    private final Map<String, ReactiveAuthorizationManager<AuthorizationContext>> securedPaths;
    private final Set<String> permitAllPaths;
    
    public EndpointSecurityScanResult(Map<String, ReactiveAuthorizationManager<AuthorizationContext>> securedPaths,
                                     Set<String> permitAllPaths) {
        this.securedPaths = securedPaths != null ? securedPaths : new HashMap<>();
        this.permitAllPaths = permitAllPaths != null ? permitAllPaths : Collections.emptySet();
    }
    
    public Map<String, ReactiveAuthorizationManager<AuthorizationContext>> getSecuredPaths() {
        return securedPaths;
    }
    
    public Set<String> getPermitAllPaths() {
        return permitAllPaths;
    }
}
