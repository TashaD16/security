package com.example.moduleb.controller;

import com.example.common.security.annotation.RequiresReadWare;
import com.example.common.security.annotation.RequiresWareInventory;
import com.example.common.security.annotation.RequiresWriteWare;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for ware operations (moduleB specific endpoints)
 */
@RestController
@RequestMapping("/api/wares")
public class WareController {

    @RequiresReadWare
    @GetMapping("/{wareId}/details")
    public Mono<ResponseEntity<Map<String, Object>>> getWareDetails(@PathVariable String wareId) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Ware details retrieved");
        response.put("wareId", wareId);
        response.put("data", Map.of("id", wareId, "status", "AVAILABLE", "quantity", 100));
        return Mono.just(ResponseEntity.ok(response));
    }

    @RequiresWareInventory
    @GetMapping("/{wareId}/inventory")
    public Mono<ResponseEntity<Map<String, Object>>> getWareInventory(@PathVariable String wareId) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Ware inventory retrieved");
        response.put("wareId", wareId);
        response.put("data", Map.of("id", wareId, "quantity", 100, "reserved", 10, "available", 90));
        return Mono.just(ResponseEntity.ok(response));
    }

    @RequiresWriteWare
    @PutMapping("/{wareId}/update")
    public Mono<ResponseEntity<Map<String, Object>>> updateWare(
            @PathVariable String wareId,
            @RequestBody Map<String, Object> ware) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Ware " + wareId + " updated successfully");
        response.put("wareId", wareId);
        response.put("data", ware);
        return Mono.just(ResponseEntity.ok(response));
    }

    @RequiresWriteWare
    @PostMapping("/{wareId}/reserve")
    public Mono<ResponseEntity<Map<String, Object>>> reserveWare(
            @PathVariable String wareId,
            @RequestBody Map<String, Object> reservation) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Ware " + wareId + " reserved");
        response.put("wareId", wareId);
        response.put("reservationId", "RES-" + wareId);
        return Mono.just(ResponseEntity.ok(response));
    }

    @RequiresWriteWare
    @PostMapping("/{wareId}/release")
    public Mono<ResponseEntity<Map<String, Object>>> releaseWare(@PathVariable String wareId) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Ware " + wareId + " released");
        response.put("wareId", wareId);
        response.put("status", "RELEASED");
        return Mono.just(ResponseEntity.ok(response));
    }
}
