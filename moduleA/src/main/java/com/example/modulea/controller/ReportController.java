package com.example.modulea.controller;

import com.example.common.security.annotation.RequiresGeneralAccess;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for report operations (common endpoints)
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @RequiresGeneralAccess
    @PostMapping("/generate")
    public Mono<ResponseEntity<Map<String, Object>>> generateReport(@RequestBody Map<String, Object> params) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Report generated successfully");
        response.put("reportId", "RPT-001");
        return Mono.just(ResponseEntity.ok(response));
    }

    @RequiresGeneralAccess
    @GetMapping("/list")
    public Mono<ResponseEntity<Map<String, Object>>> listReports() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Reports list retrieved");
        response.put("data", new String[]{"report1", "report2", "report3"});
        return Mono.just(ResponseEntity.ok(response));
    }

    @RequiresGeneralAccess
    @GetMapping("/{id}/download")
    public Mono<ResponseEntity<Map<String, Object>>> downloadReport(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Report " + id + " download initiated");
        response.put("downloadUrl", "/api/reports/" + id + "/file");
        return Mono.just(ResponseEntity.ok(response));
    }
}
