package com.example.modulea.controller;

import com.example.common.security.annotation.RequiresApproveDeclaration;
import com.example.common.security.annotation.RequiresReadDeclaration;
import com.example.common.security.annotation.RequiresWriteDeclaration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for declaration operations (moduleA specific endpoints)
 */
@RestController
@RequestMapping("/api/declarations")
public class DeclarationController {

    @RequiresReadDeclaration
    @GetMapping("/{declarationId}/details")
    public Mono<ResponseEntity<Map<String, Object>>> getDeclarationDetails(@PathVariable String declarationId) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Declaration details retrieved");
        response.put("declarationId", declarationId);
        response.put("data", Map.of("id", declarationId, "status", "PENDING"));
        return Mono.just(ResponseEntity.ok(response));
    }

    @RequiresApproveDeclaration
    @PostMapping("/{declarationId}/approve")
    public Mono<ResponseEntity<Map<String, Object>>> approveDeclaration(@PathVariable String declarationId) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Declaration " + declarationId + " approved successfully");
        response.put("declarationId", declarationId);
        response.put("status", "APPROVED");
        return Mono.just(ResponseEntity.ok(response));
    }

    @RequiresWriteDeclaration
    @PostMapping("/{declarationId}/submit")
    public Mono<ResponseEntity<Map<String, Object>>> submitDeclaration(@PathVariable String declarationId) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Declaration " + declarationId + " submitted successfully");
        response.put("declarationId", declarationId);
        response.put("status", "SUBMITTED");
        return Mono.just(ResponseEntity.ok(response));
    }

    @RequiresApproveDeclaration
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

    @RequiresWriteDeclaration
    @PostMapping("/{declarationId}/cancel")
    public Mono<ResponseEntity<Map<String, Object>>> cancelDeclaration(@PathVariable String declarationId) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Declaration " + declarationId + " cancelled");
        response.put("declarationId", declarationId);
        response.put("status", "CANCELLED");
        return Mono.just(ResponseEntity.ok(response));
    }
}
