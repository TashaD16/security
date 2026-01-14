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

/**
 * Custom Authorization Manager for handling authorization logic
 * with path variable extraction and custom access control.
 * 
 * This class provides methods that are used to create ReactiveAuthorizationManager
 * instances for Spring Security WebFlux.
 */
public class CustomAuthorizationManager {

    private static final Logger logger = LoggerFactory.getLogger(CustomAuthorizationManager.class);

    /**
     * Check read access for declarations
     */
    public Mono<AuthorizationDecision> checkReadDeclaration(
            Authentication authentication,
            AuthorizationContext context) {
        
        ServerWebExchange exchange = context.getExchange();
        String path = exchange.getRequest().getPath().value();
        String declarationId = extractPathVariable(path, "declarationId");
        
        logger.info("Checking read access for declaration: {} by user: {}", 
                declarationId, authentication.getName());
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Mono.just(new AuthorizationDecision(false));
        }
        
        // Custom logic: check if user has READ_DECLARATION authority or owns the declaration
        boolean hasAccess = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("READ_DECLARATION") ||
                             a.getAuthority().equals("ADMIN"));
        
        // Additional check: verify ownership (example logic)
        if (declarationId != null && hasAccess) {
            hasAccess = checkDeclarationOwnership(authentication.getName(), declarationId);
        }
        
        logger.debug("Read declaration access decision: {} for declaration: {}", hasAccess, declarationId);
        return Mono.just(new AuthorizationDecision(hasAccess));
    }

    /**
     * Check write access for declarations
     */
    public Mono<AuthorizationDecision> checkWriteDeclaration(
            Authentication authentication,
            AuthorizationContext context) {
        
        ServerWebExchange exchange = context.getExchange();
        String path = exchange.getRequest().getPath().value();
        String declarationId = extractPathVariable(path, "declarationId");
        
        logger.info("Checking write access for declaration: {} by user: {}", 
                declarationId, authentication.getName());
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Mono.just(new AuthorizationDecision(false));
        }
        
        boolean hasAccess = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("WRITE_DECLARATION") ||
                             a.getAuthority().equals("ADMIN"));
        
        if (declarationId != null && hasAccess) {
            hasAccess = checkDeclarationOwnership(authentication.getName(), declarationId);
        }
        
        logger.debug("Write declaration access decision: {} for declaration: {}", hasAccess, declarationId);
        return Mono.just(new AuthorizationDecision(hasAccess));
    }

    /**
     * Check access for declaration approval (moduleA specific)
     */
    public Mono<AuthorizationDecision> checkApproveDeclaration(
            Authentication authentication,
            AuthorizationContext context) {
        
        ServerWebExchange exchange = context.getExchange();
        String path = exchange.getRequest().getPath().value();
        String declarationId = extractPathVariable(path, "declarationId");
        
        logger.info("Checking approve access for declaration: {} by user: {}", 
                declarationId, authentication.getName());
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Mono.just(new AuthorizationDecision(false));
        }
        
        // Only ADMIN or APPROVER role can approve declarations
        boolean hasAccess = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("APPROVE_DECLARATION") ||
                             a.getAuthority().equals("ADMIN"));
        
        logger.debug("Approve declaration access decision: {} for declaration: {}", hasAccess, declarationId);
        return Mono.just(new AuthorizationDecision(hasAccess));
    }

    /**
     * Check read access for wares
     */
    public Mono<AuthorizationDecision> checkReadWare(
            Authentication authentication,
            AuthorizationContext context) {
        
        ServerWebExchange exchange = context.getExchange();
        String path = exchange.getRequest().getPath().value();
        String wareId = extractPathVariable(path, "wareId");
        
        logger.info("Checking read access for ware: {} by user: {}", 
                wareId, authentication.getName());
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Mono.just(new AuthorizationDecision(false));
        }
        
        boolean hasAccess = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("READ_WARE") ||
                             a.getAuthority().equals("ADMIN"));
        
        if (wareId != null && hasAccess) {
            hasAccess = checkWareOwnership(authentication.getName(), wareId);
        }
        
        logger.debug("Read ware access decision: {} for ware: {}", hasAccess, wareId);
        return Mono.just(new AuthorizationDecision(hasAccess));
    }

    /**
     * Check write access for wares
     */
    public Mono<AuthorizationDecision> checkWriteWare(
            Authentication authentication,
            AuthorizationContext context) {
        
        ServerWebExchange exchange = context.getExchange();
        String path = exchange.getRequest().getPath().value();
        String wareId = extractPathVariable(path, "wareId");
        
        logger.info("Checking write access for ware: {} by user: {}", 
                wareId, authentication.getName());
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Mono.just(new AuthorizationDecision(false));
        }
        
        boolean hasAccess = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("WRITE_WARE") ||
                             a.getAuthority().equals("ADMIN"));
        
        if (wareId != null && hasAccess) {
            hasAccess = checkWareOwnership(authentication.getName(), wareId);
        }
        
        logger.debug("Write ware access decision: {} for ware: {}", hasAccess, wareId);
        return Mono.just(new AuthorizationDecision(hasAccess));
    }

    /**
     * Check access for ware inventory (moduleB specific)
     */
    public Mono<AuthorizationDecision> checkWareInventory(
            Authentication authentication,
            AuthorizationContext context) {
        
        ServerWebExchange exchange = context.getExchange();
        String path = exchange.getRequest().getPath().value();
        String wareId = extractPathVariable(path, "wareId");
        
        logger.info("Checking inventory access for ware: {} by user: {}", 
                wareId, authentication.getName());
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Mono.just(new AuthorizationDecision(false));
        }
        
        boolean hasAccess = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("MANAGE_INVENTORY") ||
                             a.getAuthority().equals("ADMIN"));
        
        logger.debug("Ware inventory access decision: {} for ware: {}", hasAccess, wareId);
        return Mono.just(new AuthorizationDecision(hasAccess));
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
                method, path, authentication != null ? authentication.getName() : "anonymous");
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Mono.just(new AuthorizationDecision(false));
        }
        
        // Allow all authenticated users for general operations
        return Mono.just(new AuthorizationDecision(true));
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
     * Check if user owns the declaration (example implementation)
     */
    private boolean checkDeclarationOwnership(String username, String declarationId) {
        // In real implementation, this would query a database
        // For now, using simple logic: declarations starting with user's initial are owned
        if (declarationId != null && username != null && !username.isEmpty()) {
            return declarationId.startsWith(username.substring(0, 1).toUpperCase());
        }
        return true; // Default allow if we can't determine ownership
    }

    /**
     * Check if user owns the ware (example implementation)
     */
    private boolean checkWareOwnership(String username, String wareId) {
        // In real implementation, this would query a database
        // For now, using simple logic: wares starting with user's initial are owned
        if (wareId != null && username != null && !username.isEmpty()) {
            return wareId.startsWith(username.substring(0, 1).toUpperCase());
        }
        return true; // Default allow if we can't determine ownership
    }
}
