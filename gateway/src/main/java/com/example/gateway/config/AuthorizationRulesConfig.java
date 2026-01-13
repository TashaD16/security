package com.example.gateway.config;

import com.example.gateway.security.AuthorizationRule;
import com.example.gateway.security.CustomAuthorizationManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration for authorization rules mapping paths to authorization methods
 */
@Configuration
public class AuthorizationRulesConfig {

    private final CustomAuthorizationManager authorizationManager;

    public AuthorizationRulesConfig(CustomAuthorizationManager authorizationManager) {
        this.authorizationManager = authorizationManager;
    }

    @Bean
    public List<AuthorizationRule> authorizationRules() {
        return Arrays.asList(
                // Declaration endpoints - Read
                new AuthorizationRule(
                        Arrays.asList("/api/declarations/{declarationId}/details"),
                        authorizationManager::checkReadDeclaration
                ),
                
                // Declaration endpoints - Write
                new AuthorizationRule(
                        Arrays.asList(
                                "/api/declarations/{declarationId}/submit",
                                "/api/declarations/{declarationId}/cancel"
                        ),
                        authorizationManager::checkWriteDeclaration
                ),
                
                // Declaration endpoints - Approve
                new AuthorizationRule(
                        Arrays.asList(
                                "/api/declarations/{declarationId}/approve",
                                "/api/declarations/{declarationId}/reject"
                        ),
                        authorizationManager::checkApproveDeclaration
                ),
                
                // Ware endpoints - Read
                new AuthorizationRule(
                        Arrays.asList("/api/wares/{wareId}/details"),
                        authorizationManager::checkReadWare
                ),
                
                // Ware endpoints - Write
                new AuthorizationRule(
                        Arrays.asList(
                                "/api/wares/{wareId}/update",
                                "/api/wares/{wareId}/reserve",
                                "/api/wares/{wareId}/release"
                        ),
                        authorizationManager::checkWriteWare
                ),
                
                // Ware endpoints - Inventory
                new AuthorizationRule(
                        Arrays.asList("/api/wares/{wareId}/inventory"),
                        authorizationManager::checkWareInventory
                ),
                
                // General endpoints - Items
                new AuthorizationRule(
                        Arrays.asList(
                                "/api/items/list",
                                "/api/items/create",
                                "/api/items/{id}/update",
                                "/api/items/{id}/delete",
                                "/api/items/{id}/details",
                                "/api/items/search"
                        ),
                        authorizationManager::checkGeneralAccess
                ),
                
                // General endpoints - Reports
                new AuthorizationRule(
                        Arrays.asList(
                                "/api/reports/generate",
                                "/api/reports/list",
                                "/api/reports/{id}/download"
                        ),
                        authorizationManager::checkGeneralAccess
                ),
                
                // General endpoints - Data
                new AuthorizationRule(
                        Arrays.asList(
                                "/api/data/export",
                                "/api/data/import"
                        ),
                        authorizationManager::checkGeneralAccess
                )
        );
    }
}
