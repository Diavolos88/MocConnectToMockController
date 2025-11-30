package com.mock.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;

/**
 * Компонент для отправки healthcheck в MockController.
 * Отправляет информацию о состоянии заглушки каждую минуту.
 * Если healthcheck не проходит, блокирует все вызовы к MockController кроме самого healthcheck.
 */
@Component
public class HealthcheckSender {
    
    private static final Logger log = LoggerFactory.getLogger(HealthcheckSender.class);
    
    @Value("${mock-controller.url:http://localhost:8080}")
    private String mockControllerUrl;
    
    @Value("${spring.application.name}")
    private String systemName;
    
    private static final String instanceId = String.valueOf(System.nanoTime());

    private final RestTemplate restTemplate;
    private volatile boolean isMockControllerHealthy = true; // По умолчанию считаем здоровым
    private volatile long lastHealthcheckTime = 0;
    private volatile int healthcheckCount = 0;
    private volatile int healthcheckFailureCount = 0;
    
    public HealthcheckSender() {
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Отправляет healthcheck в MockController каждую минуту.
     * Если healthcheck не проходит, блокирует все вызовы к MockController.
     */
    @Scheduled(fixedRate = 60000) // Каждую минуту
    public void sendHealthcheck() {
        try {
            // Определяем instanceId один раз при запуске
            String url = mockControllerUrl + "/api/healthcheck" 
                + "?systemName=" + systemName 
                + "&instanceId=" + instanceId;
            
            lastHealthcheckTime = System.currentTimeMillis();
            healthcheckCount++;
            
            long startTime = System.currentTimeMillis();
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            long duration = System.currentTimeMillis() - startTime;
            
            // Проверяем статус 200
            int statusCode = response.getStatusCode().value();
            if (statusCode == 200) {
                isMockControllerHealthy = true;
                healthcheckFailureCount = 0;
                log.debug("Healthcheck successful: status=200, duration={}ms", duration);
            } else {
                isMockControllerHealthy = false;
                healthcheckFailureCount++;
                log.warn("Healthcheck failed: status={}, duration={}ms (failure #{})", 
                    statusCode, duration, healthcheckFailureCount);
            }
        } catch (RestClientException e) {
            isMockControllerHealthy = false;
            healthcheckFailureCount++;
            log.warn("Healthcheck failed: {} (failure #{})", e.getMessage(), healthcheckFailureCount);
        } catch (Exception e) {
            isMockControllerHealthy = false;
            healthcheckFailureCount++;
            log.error("Unexpected error during healthcheck (failure #{}): {}", 
                healthcheckFailureCount, e.getMessage(), e);
        }
    }
    
    /**
     * Проверяет, является ли MockController здоровым.
     * @return true если MockController здоров, false иначе
     */
    public boolean isMockControllerHealthy() {
        return isMockControllerHealthy;
    }
    
    /**
     * Возвращает информацию о healthcheck.
     */
    public HealthcheckInfo getHealthcheckInfo() {
        HealthcheckInfo info = new HealthcheckInfo();
        info.isHealthy = isMockControllerHealthy;
        info.lastHealthcheckTime = lastHealthcheckTime;
        info.healthcheckCount = healthcheckCount;
        info.healthcheckFailureCount = healthcheckFailureCount;
        return info;
    }
    
    /**
     * Информация о healthcheck.
     */
    public static class HealthcheckInfo {
        public boolean isHealthy;
        public long lastHealthcheckTime;
        public int healthcheckCount;
        public int healthcheckFailureCount;
    }
}

