package com.mock.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class MockService {
    
    // Параметры для Hello World эндпоинта
    private long delayHelloWorld = 1000; // задержка в миллисекундах
    private String stringHelloWorldRs = "Hello World!";
    @SuppressWarnings("unused")
    private long delayHello = 111; // задержка в миллисекундах
    @SuppressWarnings("unused")
    private String stringHell = "Hello!";

    // Параметры для Health Check эндпоинта
    private long delayHealthCheck = 500; // задержка в миллисекундах
    private String stringHealthCheckRs = "OK";
    
    public ResponseEntity<Map<String, String>> getHelloResponse() {
        try {
            Thread.sleep(delayHelloWorld);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("message", stringHelloWorldRs);
        return ResponseEntity.ok(response);
    }
    
    public ResponseEntity<Map<String, String>> getHealthResponse() {
        try {
            Thread.sleep(delayHealthCheck);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("status", stringHealthCheckRs);
        return ResponseEntity.ok(response);
    }
    
    // Геттеры и сеттеры для параметров Hello World
    public long getDelayHelloWorld() {
        return delayHelloWorld;
    }
    
    public void setDelayHelloWorld(long delayHelloWorld) {
        this.delayHelloWorld = delayHelloWorld;
    }
    
    public String getStringHelloWorldRs() {
        return stringHelloWorldRs;
    }
    
    public void setStringHelloWorldRs(String stringHelloWorldRs) {
        this.stringHelloWorldRs = stringHelloWorldRs;
    }
    
    // Геттеры и сеттеры для параметров Health Check
    public long getDelayHealthCheck() {
        return delayHealthCheck;
    }
    
    public void setDelayHealthCheck(long delayHealthCheck) {
        this.delayHealthCheck = delayHealthCheck;
    }
    
    public String getStringHealthCheckRs() {
        return stringHealthCheckRs;
    }
    
    public void setStringHealthCheckRs(String stringHealthCheckRs) {
        this.stringHealthCheckRs = stringHealthCheckRs;
    }
}

