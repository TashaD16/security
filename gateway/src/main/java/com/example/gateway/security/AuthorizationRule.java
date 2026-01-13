package com.example.gateway.security;

import java.util.List;
import java.util.function.BiFunction;
import reactor.core.publisher.Mono;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;

/**
 * Rule for path authorization mapping
 */
public class AuthorizationRule {
    
    private final List<String> pathPatterns;
    private final BiFunction<Authentication, AuthorizationContext, Mono<AuthorizationDecision>> authorizationMethod;
    
    public AuthorizationRule(List<String> pathPatterns, 
                            BiFunction<Authentication, AuthorizationContext, Mono<AuthorizationDecision>> authorizationMethod) {
        this.pathPatterns = pathPatterns;
        this.authorizationMethod = authorizationMethod;
    }
    
    public List<String> getPathPatterns() {
        return pathPatterns;
    }
    
    public BiFunction<Authentication, AuthorizationContext, Mono<AuthorizationDecision>> getAuthorizationMethod() {
        return authorizationMethod;
    }
}
