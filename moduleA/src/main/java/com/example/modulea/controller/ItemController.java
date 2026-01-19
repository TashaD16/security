package com.example.modulea.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for general item operations (common endpoints)
 * Security is configured using @PreAuthorize method-level security annotations.
 */
@RestController
@RequestMapping("/api/items")
public class ItemController {

    @PreAuthorize("@securityMethods.hasGeneralAccess(authentication)")
    @GetMapping("/list")
    public Mono<ResponseEntity<Map<String, Object>>> listItems() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Items list retrieved successfully");
        response.put("data", new String[]{"item1", "item2", "item3"});
        return Mono.just(ResponseEntity.ok(response));
    }

    @PreAuthorize("@securityMethods.hasGeneralAccess(authentication)")
    @PostMapping("/create")
    public Mono<ResponseEntity<Map<String, Object>>> createItem(@RequestBody Map<String, Object> item) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Item created successfully");
        response.put("data", item);
        return Mono.just(ResponseEntity.ok(response));
    }

    @PreAuthorize("@securityMethods.hasGeneralAccess(authentication)")
    @PutMapping("/{id}/update")
    public Mono<ResponseEntity<Map<String, Object>>> updateItem(
            @PathVariable String id,
            @RequestBody Map<String, Object> item) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Item " + id + " updated successfully");
        response.put("data", item);
        return Mono.just(ResponseEntity.ok(response));
    }

    @PreAuthorize("@securityMethods.hasGeneralAccess(authentication)")
    @DeleteMapping("/{id}/delete")
    public Mono<ResponseEntity<Map<String, Object>>> deleteItem(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Item " + id + " deleted successfully");
        return Mono.just(ResponseEntity.ok(response));
    }

    @PreAuthorize("@securityMethods.hasGeneralAccess(authentication)")
    @GetMapping("/{id}/details")
    public Mono<ResponseEntity<Map<String, Object>>> getItemDetails(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Item details retrieved for " + id);
        response.put("data", Map.of("id", id, "name", "Item " + id));
        return Mono.just(ResponseEntity.ok(response));
    }

    @PreAuthorize("@securityMethods.hasGeneralAccess(authentication)")
    @GetMapping("/search")
    public Mono<ResponseEntity<Map<String, Object>>> searchItems(@RequestParam(required = false) String query) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Search completed");
        response.put("query", query);
        response.put("data", new String[]{"result1", "result2"});
        return Mono.just(ResponseEntity.ok(response));
    }
}
