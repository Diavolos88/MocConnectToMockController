package com.mock.controller;

import com.mock.service.MockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class MockController {
    
    private final MockService mockService;
    
    @Autowired
    public MockController(MockService mockService) {
        this.mockService = mockService;
    }
    
    @GetMapping("/hello")
    public ResponseEntity<Map<String, String>> hello() {
        return mockService.getHelloResponse();
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return mockService.getHealthResponse();
    }
}

