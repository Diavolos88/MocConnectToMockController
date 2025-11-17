package com.mock.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Базовый класс для сервисов, которые хотят подключиться к MockController.
 * Наследуйтесь от этого класса, чтобы автоматически получить функциональность
 * синхронизации конфигурации с MockController.
 * 
 * Каждый наследник должен быть помечен аннотацией @Service.
 */
public class MockControllerClientBase {
    
    private static final Logger logger = LoggerFactory.getLogger(MockControllerClientBase.class);
    
    @Autowired
    private MockControllerConfig mockControllerConfig;
    
    @Autowired
    private AppConfig appConfig;
    
    @Autowired
    private LoggingConfig loggingConfig;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private String version = "v1";
    
    /**
     * Вызывается при готовности приложения.
     * Отправляет запрос checkUpdate в MockController.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        checkUpdate();
    }
    
    /**
     * Проверяет обновления конфигурации в MockController.
     * Отправляет POST запрос на /api/configs/checkUpdate.
     */
    public void checkUpdate() {
        try {
            if (mockControllerConfig == null || appConfig == null || loggingConfig == null) {
                logger.warn("MockController dependencies not initialized, skipping checkUpdate");
                return;
            }
            
            String url = mockControllerConfig.getUrl() + "/api/configs/checkUpdate";
            
            // Формируем запрос
            Map<String, Object> requestBody = buildCheckUpdateRequest();
            
            // Настраиваем заголовки
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // Отправляем запрос
            ResponseEntity<CheckUpdateResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                CheckUpdateResponse.class
            );
            
            CheckUpdateResponse responseBody = response.getBody();
            
            if (responseBody != null) {
                // Обновляем версию из ответа
                if (responseBody.getCurrentVersion() != null) {
                    this.version = responseBody.getCurrentVersion();
                }
                
                // Если требуется обновление, загружаем и применяем новый конфиг
                if (responseBody.isNeedUpdate()) {
                    logger.info("Config update required, loading version: {}", responseBody.getCurrentVersion());
                    loadAndApplyConfig(responseBody.getCurrentVersion());
                }
            } else {
                logger.warn("Empty response received from MockController");
            }
            
        } catch (Exception e) {
            logger.error("Error calling checkUpdate in MockController: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Периодически проверяет обновления конфигурации.
     * Интервал задается в application.yml (mock-controller.check-interval-seconds).
     */
    @Scheduled(fixedDelayString = "${mock-controller.check-interval-seconds:5}000")
    public void scheduledCheckUpdate() {
        checkUpdate();
    }
    
    /**
     * Загружает конфигурацию из MockController по версии и применяет её к текущему объекту.
     * 
     * @param version версия конфига для загрузки
     */
    private void loadAndApplyConfig(String version) {
        try {
            if (mockControllerConfig == null || appConfig == null) {
                logger.warn("MockController dependencies not initialized, cannot load config");
                return;
            }
            
            String systemName = appConfig.getName();
            String url = mockControllerConfig.getUrl() + "/api/configs/" + systemName + "?version=" + version;
            
            // Отправляем GET запрос
            ResponseEntity<ConfigResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                ConfigResponse.class
            );
            
            ConfigResponse configResponse = response.getBody();
            
            if (configResponse != null && configResponse.getConfig() != null) {
                // Применяем конфиг к текущему объекту
                applyConfig(configResponse.getConfig());
                
                // Обновляем версию
                this.version = configResponse.getVersion();
                logger.info("Config applied successfully, version: {}", configResponse.getVersion());
            } else {
                logger.warn("Empty response received when loading config");
            }
            
        } catch (Exception e) {
            logger.error("Error loading config from MockController: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Применяет конфигурацию к текущему объекту через рефлексию.
     * Обновляет все поля, начинающиеся с "delay" и "string".
     * 
     * @param config конфигурация из MockController
     */
    @SuppressWarnings("unchecked")
    private void applyConfig(Map<String, Object> config) {
        try {
            Class<?> clazz = this.getClass();
            Field[] fields = clazz.getDeclaredFields();
            
            // Применяем delays
            if (config.containsKey("delays")) {
                Map<String, Object> delays = (Map<String, Object>) config.get("delays");
                
                for (Map.Entry<String, Object> entry : delays.entrySet()) {
                    String fieldName = entry.getKey();
                    Field field = findField(fields, fieldName);
                    
                    if (field != null) {
                        field.setAccessible(true);
                        String stringValue = String.valueOf(entry.getValue());
                        Object value = parseValue(field.getType(), stringValue);
                        field.set(this, value);
                    } else {
                        logger.warn("Field {} not found in {}, skipping", fieldName, clazz.getSimpleName());
                    }
                }
            }
            
            // Применяем stringParams
            if (config.containsKey("stringParams")) {
                Map<String, Object> stringParams = (Map<String, Object>) config.get("stringParams");
                
                for (Map.Entry<String, Object> entry : stringParams.entrySet()) {
                    String fieldName = entry.getKey();
                    Field field = findField(fields, fieldName);
                    
                    if (field != null) {
                        field.setAccessible(true);
                        String stringValue = String.valueOf(entry.getValue());
                        field.set(this, stringValue);
                    } else {
                        logger.warn("Field {} not found in {}, skipping", fieldName, clazz.getSimpleName());
                    }
                }
            }
            
            // Обновляем loggingLv
            if (config.containsKey("loggingLv")) {
                String loggingLv = String.valueOf(config.get("loggingLv"));
                applyLoggingLevel(loggingLv);
                loggingConfig.setLoggingLevel(loggingLv);
            }
            
        } catch (Exception e) {
            logger.error("Error applying config to {}: {}", this.getClass().getSimpleName(), e.getMessage(), e);
        }
    }
    
    /**
     * Извлекает все поля, начинающиеся с "delay" из текущего объекта через рефлексию.
     */
    private Map<String, String> extractDelays() {
        Map<String, String> delays = new HashMap<>();
        
        try {
            Class<?> clazz = this.getClass();
            Field[] fields = clazz.getDeclaredFields();
            
            for (Field field : fields) {
                if (field.getName().startsWith("delay")) {
                    field.setAccessible(true);
                    Object value = field.get(this);
                    delays.put(field.getName(), String.valueOf(value));
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error extracting delays from " + this.getClass().getSimpleName(), e);
        }
        
        return delays;
    }
    
    /**
     * Извлекает все поля, начинающиеся с "string" из текущего объекта через рефлексию.
     */
    private Map<String, String> extractStringParams() {
        Map<String, String> stringParams = new HashMap<>();
        
        try {
            Class<?> clazz = this.getClass();
            Field[] fields = clazz.getDeclaredFields();
            
            for (Field field : fields) {
                if (field.getName().startsWith("string")) {
                    field.setAccessible(true);
                    Object value = field.get(this);
                    stringParams.put(field.getName(), String.valueOf(value));
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error extracting stringParams from " + this.getClass().getSimpleName(), e);
        }
        
        return stringParams;
    }
    
    /**
     * Формирует полный конфиг для отправки в MockController.
     */
    private Map<String, Object> buildConfig() {
        Map<String, Object> config = new HashMap<>();
        
        Map<String, String> delays = extractDelays();
        Map<String, String> stringParams = extractStringParams();
        
        config.put("delays", delays);
        config.put("stringParams", stringParams);
        config.put("loggingLv", loggingConfig.getLoggingLevel());
        
        return config;
    }
    
    /**
     * Формирует полный запрос для отправки в MockController (checkUpdate).
     */
    private Map<String, Object> buildCheckUpdateRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put("SystemName", appConfig.getName());
        request.put("version", version);
        request.put("config", buildConfig());
        return request;
    }
    
    /**
     * Находит поле по точному имени.
     */
    private Field findField(Field[] fields, String fieldName) {
        for (Field field : fields) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }
    
    /**
     * Парсит значение в зависимости от типа поля.
     */
    private Object parseValue(Class<?> type, String value) {
        if (type == long.class || type == Long.class) {
            return Long.parseLong(value);
        } else if (type == int.class || type == Integer.class) {
            return Integer.parseInt(value);
        } else if (type == String.class) {
            return value;
        }
        return value;
    }
    
    /**
     * Применяет уровень логирования к пакету, указанному в LoggingConfig.
     * 
     * @param levelStr уровень логирования (ERROR, WARN, INFO, DEBUG)
     */
    private void applyLoggingLevel(String levelStr) {
        try {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            // Используем пакет текущего класса для применения уровня логирования
            ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(this.getClass().getPackage().getName());
            
            Level level;
            switch (levelStr.toUpperCase()) {
                case "ERROR":
                    level = Level.ERROR;
                    break;
                case "WARN":
                    level = Level.WARN;
                    break;
                case "INFO":
                    level = Level.INFO;
                    break;
                case "DEBUG":
                    level = Level.DEBUG;
                    break;
                case "TRACE":
                    level = Level.TRACE;
                    break;
                default:
                    logger.warn("Unknown logging level: {}, using INFO", levelStr);
                    level = Level.INFO;
                    break;
            }
            
            rootLogger.setLevel(level);
        } catch (Exception e) {
            logger.error("Error setting logging level: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Класс для десериализации ответа от checkUpdate API.
     */
    public static class CheckUpdateResponse {
        private boolean needUpdate;
        private String currentVersion;
        
        public boolean isNeedUpdate() {
            return needUpdate;
        }
        
        public void setNeedUpdate(boolean needUpdate) {
            this.needUpdate = needUpdate;
        }
        
        public String getCurrentVersion() {
            return currentVersion;
        }
        
        public void setCurrentVersion(String currentVersion) {
            this.currentVersion = currentVersion;
        }
    }
    
    /**
     * Класс для десериализации ответа от GET /api/configs/{systemName} API.
     */
    public static class ConfigResponse {
        private String systemName;
        private String version;
        private Map<String, Object> config;
        private String updatedAt;
        
        public String getSystemName() {
            return systemName;
        }
        
        public void setSystemName(String systemName) {
            this.systemName = systemName;
        }
        
        public String getVersion() {
            return version;
        }
        
        public void setVersion(String version) {
            this.version = version;
        }
        
        public Map<String, Object> getConfig() {
            return config;
        }
        
        public void setConfig(Map<String, Object> config) {
            this.config = config;
        }
        
        public String getUpdatedAt() {
            return updatedAt;
        }
        
        public void setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
        }
    }
}

