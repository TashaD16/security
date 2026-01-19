package com.example.gateway.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Helper class for authorization logic.
 * Provides utility methods that can be used in security expressions.
 */
@Component
public class CustomAuthorizationHelper {

    private static final String AUTHORITY_ADMIN = "ADMIN";

    /**
     * Check if user has the required authority or ADMIN authority
     */
    public boolean hasAuthority(Authentication authentication, String requiredAuthority) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(requiredAuthority) || a.equals(AUTHORITY_ADMIN));
    }

    /**
     * Check if user owns the resource by username and resource ID.
     * In real implementation, this would query a database.
     * For now, using simple logic: resources starting with user's initial are owned.
     */
    public boolean checkOwnership(String username, String resourceId) {
        if (resourceId != null && username != null && !username.isEmpty()) {
            return resourceId.startsWith(username.substring(0, 1).toUpperCase());
        }
        return true; // Default allow if we can't determine ownership
    }
}
