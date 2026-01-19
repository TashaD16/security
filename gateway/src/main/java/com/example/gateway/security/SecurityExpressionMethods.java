package com.example.gateway.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Custom security expression methods for use in @PreAuthorize SpEL expressions.
 * These methods can be accessed in SpEL as @securityMethods.methodName()
 */
@Component("securityMethods")
public class SecurityExpressionMethods {

    private final CustomAuthorizationHelper authHelper;

    public SecurityExpressionMethods(CustomAuthorizationHelper authHelper) {
        this.authHelper = authHelper;
    }

    /**
     * Check if user has READ_DECLARATION authority and owns the declaration
     */
    public boolean canReadDeclaration(String declarationId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authHelper.hasAuthority(authentication, "READ_DECLARATION") &&
               authHelper.checkOwnership(authentication.getName(), declarationId);
    }

    /**
     * Check if user has WRITE_DECLARATION authority and owns the declaration
     */
    public boolean canWriteDeclaration(String declarationId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authHelper.hasAuthority(authentication, "WRITE_DECLARATION") &&
               authHelper.checkOwnership(authentication.getName(), declarationId);
    }

    /**
     * Check if user has APPROVE_DECLARATION authority
     */
    public boolean canApproveDeclaration(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authHelper.hasAuthority(authentication, "APPROVE_DECLARATION");
    }

    /**
     * Check if user has READ_WARE authority and owns the ware
     */
    public boolean canReadWare(String wareId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authHelper.hasAuthority(authentication, "READ_WARE") &&
               authHelper.checkOwnership(authentication.getName(), wareId);
    }

    /**
     * Check if user has WRITE_WARE authority and owns the ware
     */
    public boolean canWriteWare(String wareId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authHelper.hasAuthority(authentication, "WRITE_WARE") &&
               authHelper.checkOwnership(authentication.getName(), wareId);
    }

    /**
     * Check if user has MANAGE_INVENTORY authority
     */
    public boolean canManageInventory(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authHelper.hasAuthority(authentication, "MANAGE_INVENTORY");
    }

}
