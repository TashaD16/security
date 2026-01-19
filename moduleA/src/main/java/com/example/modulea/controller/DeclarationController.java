package com.example.modulea.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for declaration operations (moduleA specific endpoints)
 * Security is configured using @PreAuthorize method-level security annotations.
 */
@RestController
@RequestMapping("/api/declarations")
public class DeclarationController {

    @PreAuthorize("@securityMethods.canReadDeclaration(#declarationId, authentication)")
    @GetMapping("/{declarationId}/details")
    public Mono<ResponseEntity<Map<String, Object>>> getDeclarationDetails(@PathVariable String declarationId) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Declaration details retrieved");
        response.put("declarationId", declarationId);
        response.put("data", Map.of("id", declarationId, "status", "PENDING"));
        return Mono.just(ResponseEntity.ok(response));
    }

    @PreAuthorize("@securityMethods.canApproveDeclaration(authentication)")
    @PostMapping("/{declarationId}/approve")
    public Mono<ResponseEntity<Map<String, Object>>> approveDeclaration(@PathVariable String declarationId) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Declaration " + declarationId + " approved successfully");
        response.put("declarationId", declarationId);
        response.put("status", "APPROVED");
        return Mono.just(ResponseEntity.ok(response));
    }

    @PreAuthorize("@securityMethods.canWriteDeclaration(#declarationId, authentication)")
    @PostMapping("/{declarationId}/submit")
    public Mono<ResponseEntity<Map<String, Object>>> submitDeclaration(@PathVariable String declarationId) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Declaration " + declarationId + " submitted successfully");
        response.put("declarationId", declarationId);
        response.put("status", "SUBMITTED");
        return Mono.just(ResponseEntity.ok(response));
    }

    @PreAuthorize("@securityMethods.canApproveDeclaration(authentication)")
    @PostMapping("/{declarationId}/reject")
    public Mono<ResponseEntity<Map<String, Object>>> rejectDeclaration(
            @PathVariable String declarationId,
            @RequestBody(required = false) Map<String, Object> reason) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Declaration " + declarationId + " rejected");
        response.put("declarationId", declarationId);
        response.put("status", "REJECTED");
        response.put("reason", reason);
        return Mono.just(ResponseEntity.ok(response));
    }

    @PreAuthorize("@securityMethods.canWriteDeclaration(#declarationId, authentication)")
    @PostMapping("/{declarationId}/cancel")
    public Mono<ResponseEntity<Map<String, Object>>> cancelDeclaration(@PathVariable String declarationId) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Declaration " + declarationId + " cancelled");
        response.put("declarationId", declarationId);
        response.put("status", "CANCELLED");
        return Mono.just(ResponseEntity.ok(response));
    }
}
