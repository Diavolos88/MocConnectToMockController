package com.mock.config;

import com.mock.service.MockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Класс для извлечения конфигурации из MockService через рефлексию.
 * Извлекает все поля, начинающиеся с "delay" (delays) и "string" (stringParams).
 */
@Component
public class MockConfigExtractor {
    
    private final MockService mockService;
    private final AppConfig appConfig;
    private final LoggingConfig loggingConfig;
    
    private String version = "v1";
    
    @Autowired
    public MockConfigExtractor(MockService mockService, AppConfig appConfig, LoggingConfig loggingConfig) {
        this.mockService = mockService;
        this.appConfig = appConfig;
        this.loggingConfig = loggingConfig;
    }
    
    /**
     * Получает SystemName из application.yml (spring.application.name)
     */
    public String getSystemName() {
        return appConfig.getName();
    }
    
    /**
     * Получает loggingLv из application.yml (logging.level.com.mock)
     */
    public String getLoggingLv() {
        return loggingConfig.getComMock();
    }
    
    /**
     * Получает текущую версию конфига
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * Устанавливает версию конфига (будет обновляться из MockController)
     */
    public void setVersion(String version) {
        this.version = version;
    }
    
    /**
     * Извлекает все поля, начинающиеся с "delay" из MockService через рефлексию.
     * Возвращает Map где ключ - полное имя поля, значение - значение поля как String.
     */
    public Map<String, String> extractDelays() {
        Map<String, String> delays = new HashMap<>();
        
        try {
            Class<?> clazz = mockService.getClass();
            Field[] fields = clazz.getDeclaredFields();
            
            for (Field field : fields) {
                if (field.getName().startsWith("delay")) {
                    field.setAccessible(true);
                    Object value = field.get(mockService);
                    delays.put(field.getName(), String.valueOf(value));
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error extracting delays from MockService", e);
        }
        
        return delays;
    }
    
    /**
     * Извлекает все поля, начинающиеся с "string" из MockService через рефлексию.
     * Возвращает Map где ключ - полное имя поля, значение - значение поля как String.
     */
    public Map<String, String> extractStringParams() {
        Map<String, String> stringParams = new HashMap<>();
        
        try {
            Class<?> clazz = mockService.getClass();
            Field[] fields = clazz.getDeclaredFields();
            
            for (Field field : fields) {
                if (field.getName().startsWith("string")) {
                    field.setAccessible(true);
                    Object value = field.get(mockService);
                    stringParams.put(field.getName(), String.valueOf(value));
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error extracting stringParams from MockService", e);
        }
        
        return stringParams;
    }
    
    /**
     * Формирует полный конфиг для отправки в MockController.
     * Возвращает Map в формате, соответствующем API MockController.
     */
    public Map<String, Object> buildConfig() {
        Map<String, Object> config = new HashMap<>();
        
        Map<String, String> delays = extractDelays();
        Map<String, String> stringParams = extractStringParams();
        
        config.put("delays", delays);
        config.put("stringParams", stringParams);
        config.put("loggingLv", getLoggingLv());
        
        return config;
    }
    
    /**
     * Формирует полный запрос для отправки в MockController (checkUpdate).
     * Включает SystemName, version и config.
     */
    public Map<String, Object> buildCheckUpdateRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put("SystemName", getSystemName());
        request.put("version", getVersion());
        request.put("config", buildConfig());
        return request;
    }
}

