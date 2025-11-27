package com.mock.service;

import com.mock.config.MockControllerClientBase;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class MockService extends MockControllerClientBase {
    
    // Параметры для Hello World эндпоинта
    private long delayHelloWorld = 1000; // задержка в миллисекундах
    private String stringHelloWorldRs = "Hello World!";
    private int intHelloStatusCode = 200; // HTTP статус код для hello
    private int intResponseValue = 5030; // Пример числового значения
    @SuppressWarnings("unused")
    private long delayHello = 111; // задержка в миллисекундах
    @SuppressWarnings("unused")
    private String stringHell = "Hello!";

    // Параметры для Health Check эндпоинта
    private long delayHealthCheck = 500; // задержка в миллисекундах
    private String stringHealthCheckRs = "OK";
    private int intHealthStatusCode = 200; // HTTP статус код
    private boolean isHealthTrue = true; // Boolean параметр для health
    
    public ResponseEntity<Map<String, String>> getHelloResponse() {
        try {
            Thread.sleep(delayHelloWorld);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("message", stringHelloWorldRs);
        response.put("responseValue", String.valueOf(intResponseValue));
        return ResponseEntity.status(intHelloStatusCode).body(response);
    }
    
    public ResponseEntity<Map<String, String>> getHealthResponse() {
        try {
            Thread.sleep(delayHealthCheck);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("status", stringHealthCheckRs);
        response.put("isHealthy", String.valueOf(isHealthTrue));
        return ResponseEntity.status(intHealthStatusCode).body(response);
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
    
    // Геттеры и сеттеры для int параметров
    public int getIntHealthStatusCode() {
        return intHealthStatusCode;
    }
    
    public void setIntHealthStatusCode(int intHealthStatusCode) {
        this.intHealthStatusCode = intHealthStatusCode;
    }
    
    public int getIntHelloStatusCode() {
        return intHelloStatusCode;
    }
    
    public void setIntHelloStatusCode(int intHelloStatusCode) {
        this.intHelloStatusCode = intHelloStatusCode;
    }
    
    public int getIntResponseValue() {
        return intResponseValue;
    }
    
    public void setIntResponseValue(int intResponseValue) {
        this.intResponseValue = intResponseValue;
    }
    
    // Геттеры и сеттеры для boolean параметров
    public boolean isHealthTrue() {
        return isHealthTrue;
    }
    
    public void setHealthTrue(boolean healthTrue) {
        isHealthTrue = healthTrue;
    }
}

