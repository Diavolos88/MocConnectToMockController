package com.mock.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;

/**
 * Компонент для отправки healthcheck в MockController.
 * Отправляет информацию о состоянии заглушки каждую минуту.
 */
@Component
public class HealthcheckSender {
    
    private static final Logger log = LoggerFactory.getLogger(HealthcheckSender.class);
    
    @Value("${mock-controller.url:http://localhost:8080}")
    private String mockControllerUrl;
    
    @Value("${spring.application.name}")
    private String systemName;
    
    private final RestTemplate restTemplate;
    
    public HealthcheckSender() {
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Отправляет healthcheck в MockController каждую минуту.
     */
    @Scheduled(fixedRate = 60000) // Каждую минуту
    public void sendHealthcheck() {
        try {
            String instanceId = InetAddress.getLocalHost().getHostName();
            String url = mockControllerUrl + "/api/healthcheck" 
                + "?systemName=" + systemName 
                + "&instanceId=" + instanceId;
            
            restTemplate.postForObject(url, null, String.class);
            log.debug("Healthcheck sent successfully to MockController: {}", url);
        } catch (Exception e) {
            // Логируем ошибку, но не падаем
            log.warn("Failed to send healthcheck to MockController: {}", e.getMessage());
        }
    }
}

