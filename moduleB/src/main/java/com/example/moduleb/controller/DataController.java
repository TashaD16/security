package com.example.moduleb.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for data operations (common endpoints)
 * Security is configured using @PreAuthorize method-level security annotations.
 */
@RestController
@RequestMapping("/api/data")
public class DataController {

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/export")
    public Mono<ResponseEntity<Map<String, Object>>> exportData(@RequestBody Map<String, Object> params) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Data export started");
        response.put("exportId", "EXP-001");
        return Mono.just(ResponseEntity.ok(response));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/import")
    public Mono<ResponseEntity<Map<String, Object>>> importData(@RequestBody Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Data import completed");
        response.put("imported", 100);
        return Mono.just(ResponseEntity.ok(response));
    }
}
