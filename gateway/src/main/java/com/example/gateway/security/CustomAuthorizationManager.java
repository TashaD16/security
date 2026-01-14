package com.example.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.BiFunction;

/**
 * Custom Authorization Manager for handling authorization logic
 * with path variable extraction and custom access control.
 * 
 * This class provides methods that are used to create ReactiveAuthorizationManager
 * instances for Spring Security WebFlux.
 */
public class CustomAuthorizationManager {

    private static final Logger logger = LoggerFactory.getLogger(CustomAuthorizationManager.class);

    // Authority constants
    private static final String AUTHORITY_ADMIN = "ADMIN";
    private static final String AUTHORITY_READ_DECLARATION = "READ_DECLARATION";
    private static final String AUTHORITY_WRITE_DECLARATION = "WRITE_DECLARATION";
    private static final String AUTHORITY_APPROVE_DECLARATION = "APPROVE_DECLARATION";
    private static final String AUTHORITY_READ_WARE = "READ_WARE";
    private static final String AUTHORITY_WRITE_WARE = "WRITE_WARE";
    private static final String AUTHORITY_MANAGE_INVENTORY = "MANAGE_INVENTORY";

    // Path variable names
    private static final String PATH_VAR_DECLARATION_ID = "declarationId";
    private static final String PATH_VAR_WARE_ID = "wareId";

    /**
     * Check read access for declarations
     */
    public Mono<AuthorizationDecision> checkReadDeclaration(
            Authentication authentication,
            AuthorizationContext context) {
        
        String declarationId = extractPathVariableFromContext(context, PATH_VAR_DECLARATION_ID);
        logger.info("Checking read access for declaration: {} by user: {}", 
                declarationId, getUsername(authentication));
        
        return checkAccess(authentication, AUTHORITY_READ_DECLARATION, declarationId,
                this::checkResourceOwnership);
    }

    /**
     * Check write access for declarations
     */
    public Mono<AuthorizationDecision> checkWriteDeclaration(
            Authentication authentication,
            AuthorizationContext context) {
        
        String declarationId = extractPathVariableFromContext(context, PATH_VAR_DECLARATION_ID);
        logger.info("Checking write access for declaration: {} by user: {}", 
                declarationId, getUsername(authentication));
        
        return checkAccess(authentication, AUTHORITY_WRITE_DECLARATION, declarationId,
                this::checkResourceOwnership);
    }

    /**
     * Check access for declaration approval (moduleA specific)
     */
    public Mono<AuthorizationDecision> checkApproveDeclaration(
            Authentication authentication,
            AuthorizationContext context) {
        
        String declarationId = extractPathVariableFromContext(context, PATH_VAR_DECLARATION_ID);
        logger.info("Checking approve access for declaration: {} by user: {}", 
                declarationId, getUsername(authentication));
        
        return checkAccess(authentication, AUTHORITY_APPROVE_DECLARATION, null, null);
    }

    /**
     * Check read access for wares
     */
    public Mono<AuthorizationDecision> checkReadWare(
            Authentication authentication,
            AuthorizationContext context) {
        
        String wareId = extractPathVariableFromContext(context, PATH_VAR_WARE_ID);
        logger.info("Checking read access for ware: {} by user: {}", 
                wareId, getUsername(authentication));
        
        return checkAccess(authentication, AUTHORITY_READ_WARE, wareId,
                this::checkResourceOwnership);
    }

    /**
     * Check write access for wares
     */
    public Mono<AuthorizationDecision> checkWriteWare(
            Authentication authentication,
            AuthorizationContext context) {
        
        String wareId = extractPathVariableFromContext(context, PATH_VAR_WARE_ID);
        logger.info("Checking write access for ware: {} by user: {}", 
                wareId, getUsername(authentication));
        
        return checkAccess(authentication, AUTHORITY_WRITE_WARE, wareId,
                this::checkResourceOwnership);
    }

    /**
     * Check access for ware inventory (moduleB specific)
     */
    public Mono<AuthorizationDecision> checkWareInventory(
            Authentication authentication,
            AuthorizationContext context) {
        
        String wareId = extractPathVariableFromContext(context, PATH_VAR_WARE_ID);
        logger.info("Checking inventory access for ware: {} by user: {}", 
                wareId, getUsername(authentication));
        
        return checkAccess(authentication, AUTHORITY_MANAGE_INVENTORY, null, null);
    }

    /**
     * Check access for general CRUD operations
     */
    public Mono<AuthorizationDecision> checkGeneralAccess(
            Authentication authentication,
            AuthorizationContext context) {
        
        ServerWebExchange exchange = context.getExchange();
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        
        logger.debug("Checking general access for {} {} by user: {}", 
                method, path, getUsername(authentication));
        
        if (!isAuthenticated(authentication)) {
            return Mono.just(new AuthorizationDecision(false));
        }
        
        // Allow all authenticated users for general operations
        return Mono.just(new AuthorizationDecision(true));
    }

    /**
     * Common method for checking access with authority and optional ownership check
     */
    private Mono<AuthorizationDecision> checkAccess(
            Authentication authentication,
            String requiredAuthority,
            String resourceId,
            BiFunction<String, String, Boolean> ownershipChecker) {
        
        if (!isAuthenticated(authentication)) {
            return Mono.just(new AuthorizationDecision(false));
        }
        
        boolean hasAccess = hasAuthority(authentication, requiredAuthority);
        
        // Additional ownership check if resource ID and checker are provided
        if (resourceId != null && ownershipChecker != null && hasAccess) {
            String username = authentication.getName();
            hasAccess = ownershipChecker.apply(username, resourceId);
        }
        
        logger.debug("Access decision: {} for resource: {}", hasAccess, resourceId);
        return Mono.just(new AuthorizationDecision(hasAccess));
    }

    /**
     * Check if user is authenticated
     */
    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated();
    }

    /**
     * Check if user has the required authority or ADMIN authority
     */
    private boolean hasAuthority(Authentication authentication, String requiredAuthority) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(requiredAuthority) ||
                             a.getAuthority().equals(AUTHORITY_ADMIN));
    }

    /**
     * Extract path variable from authorization context
     */
    private String extractPathVariableFromContext(AuthorizationContext context, String variableName) {
        ServerWebExchange exchange = context.getExchange();
        String path = exchange.getRequest().getPath().value();
        return extractPathVariable(path, variableName);
    }

    /**
     * Extract path variable from URI path
     */
    private String extractPathVariable(String path, String variableName) {
        // Pattern to match /{variableName}/ or /{variableName} at end
        Pattern pattern = Pattern.compile("/" + variableName + "/([^/]+)");
        Matcher matcher = pattern.matcher(path);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Try to match at the end of path
        pattern = Pattern.compile("/" + variableName + "/([^/?]+)");
        matcher = pattern.matcher(path);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }

    /**
     * Check if user owns the resource by username and resource ID (unified method for declarations and wares)
     * In real implementation, this would query a database
     * For now, using simple logic: resources starting with user's initial are owned
     */
    private boolean checkResourceOwnership(String username, String resourceId) {
        if (resourceId != null && username != null && !username.isEmpty()) {
            return resourceId.startsWith(username.substring(0, 1).toUpperCase());
        }
        return true; // Default allow if we can't determine ownership
    }

    /**
     * Get username from authentication or return "anonymous"
     */
    private String getUsername(Authentication authentication) {
        return authentication != null ? authentication.getName() : "anonymous";
    }
}
