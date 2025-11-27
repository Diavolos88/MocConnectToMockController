package com.mock.controller;

import com.mock.config.ConfigAggregator;
import com.mock.service.MockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class MockController {
    
    private final MockService mockService;
    private final ConfigAggregator configAggregator;
    
    @Autowired
    public MockController(MockService mockService, ConfigAggregator configAggregator) {
        this.mockService = mockService;
        this.configAggregator = configAggregator;
    }
    
    @GetMapping("/hello")
    public ResponseEntity<Map<String, String>> hello() {
        return mockService.getHelloResponse();
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return mockService.getHealthResponse();
    }
    
    @GetMapping("/config/status")
    public ResponseEntity<Map<String, Object>> getConfigStatus() {
        return ResponseEntity.ok(configAggregator.getCheckUpdateStatus());
    }
}

