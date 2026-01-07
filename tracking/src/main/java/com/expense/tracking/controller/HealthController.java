package com.expense.tracking.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
public class HealthController {
    
    /**
     * Simple health check endpoint for connectivity testing
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "expense-tracking");
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * HEAD request for lightweight connectivity check
     */
    @RequestMapping(value = "/health", method = org.springframework.web.bind.annotation.RequestMethod.HEAD)
    public ResponseEntity<Void> healthCheckHead() {
        return ResponseEntity.ok().build();
    }
}