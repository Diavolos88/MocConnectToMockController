package com.mock.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Централизованный компонент для сбора конфигурации от всех сервисов,
 * наследующихся от MockControllerClientBase, и синхронизации с MockController.
 */
@Component
public class ConfigAggregator {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigAggregator.class);
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private MockControllerConfig mockControllerConfig;
    
    @Autowired
    private AppConfig appConfig;
    
    @Autowired
    private LoggingConfig loggingConfig;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private String version = "v1";
    private long lastCheckUpdateTime = 0;
    private int checkUpdateCount = 0;
    
    /**
     * Находит все сервисы, наследующиеся от MockControllerClientBase.
     */
    private List<MockControllerClientBase> getAllConfigurableServices() {
        List<MockControllerClientBase> services = new ArrayList<>();
        Map<String, MockControllerClientBase> beans = applicationContext.getBeansOfType(MockControllerClientBase.class);
        services.addAll(beans.values());
        logger.debug("Found {} configurable services", services.size());
        return services;
    }
    
    /**
     * Собирает конфигурацию от всех сервисов в один общий конфиг.
     */
    private Map<String, Object> buildAggregatedConfig() {
        Map<String, Object> config = new HashMap<>();
        Map<String, String> delays = new HashMap<>();
        Map<String, String> intParams = new HashMap<>();
        Map<String, String> stringParams = new HashMap<>();
        Map<String, String> booleanVariables = new HashMap<>();
        
        List<MockControllerClientBase> services = getAllConfigurableServices();
        
        for (MockControllerClientBase service : services) {
            String serviceName = service.getClass().getSimpleName();
            logger.debug("Collecting config from service: {}", serviceName);
            
            // Извлекаем delays
            Map<String, String> serviceDelays = extractDelays(service);
            delays.putAll(serviceDelays);
            
            // Извлекаем intParams
            Map<String, String> serviceIntParams = extractIntParams(service);
            intParams.putAll(serviceIntParams);
            
            // Извлекаем stringParams
            Map<String, String> serviceStringParams = extractStringParams(service);
            stringParams.putAll(serviceStringParams);
            
            // Извлекаем booleanVariables
            Map<String, String> serviceBooleanVariables = extractBooleanVariables(service);
            booleanVariables.putAll(serviceBooleanVariables);
        }
        
        config.put("delays", delays);
        config.put("intParams", intParams);
        config.put("stringParams", stringParams);
        config.put("booleanVariables", booleanVariables);
        config.put("loggingLv", loggingConfig.getLoggingLevel());
        
        logger.debug("Aggregated config: delays={}, intParams={}, stringParams={}, booleanVariables={}", 
            delays.size(), intParams.size(), stringParams.size(), booleanVariables.size());
        
        return config;
    }
    
    /**
     * Универсальный метод для извлечения полей с заданным префиксом из сервиса.
     * Поддерживает как маленькую, так и большую букву в начале префикса.
     */
    private Map<String, String> extractFieldsByPrefix(MockControllerClientBase service, String prefix) {
        Map<String, String> result = new HashMap<>();
        
        try {
            Class<?> clazz = service.getClass();
            Field[] fields = clazz.getDeclaredFields();
            
            // Создаем варианты префикса с разным регистром
            String prefixLower = prefix.toLowerCase();
            String prefixUpper = prefix.toUpperCase();
            String prefixCapitalized = prefix.substring(0, 1).toUpperCase() + prefix.substring(1).toLowerCase();
            
            for (Field field : fields) {
                String fieldName = field.getName();
                // Проверяем все варианты регистра
                if (fieldName.startsWith(prefixLower) || 
                    fieldName.startsWith(prefixUpper) || 
                    fieldName.startsWith(prefixCapitalized)) {
                    field.setAccessible(true);
                    Object value = field.get(service);
                    result.put(field.getName(), String.valueOf(value));
                }
            }
        } catch (IllegalAccessException e) {
            logger.error("Error extracting fields with prefix '{}' from {}: {}", 
                prefix, service.getClass().getSimpleName(), e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Извлекает все поля, начинающиеся с "delay", "Delay" или "DELAY" из сервиса.
     */
    private Map<String, String> extractDelays(MockControllerClientBase service) {
        return extractFieldsByPrefix(service, "delay");
    }
    
    /**
     * Извлекает все поля, начинающиеся с "int", "Int" или "INT" из сервиса.
     */
    private Map<String, String> extractIntParams(MockControllerClientBase service) {
        return extractFieldsByPrefix(service, "int");
    }
    
    /**
     * Извлекает все поля, начинающиеся с "string", "String" или "STRING" из сервиса.
     */
    private Map<String, String> extractStringParams(MockControllerClientBase service) {
        return extractFieldsByPrefix(service, "string");
    }
    
    /**
     * Извлекает все поля, начинающиеся с "is", "Is" или "IS" из сервиса.
     */
    private Map<String, String> extractBooleanVariables(MockControllerClientBase service) {
        return extractFieldsByPrefix(service, "is");
    }
    
    /**
     * Применяет конфигурацию ко всем сервисам.
     */
    private void applyConfigToAllServices(Map<String, Object> config) {
        List<MockControllerClientBase> services = getAllConfigurableServices();
        
        for (MockControllerClientBase service : services) {
            applyConfigToService(service, config);
        }
    }
    
    /**
     * Применяет конфигурацию к конкретному сервису.
     */
    @SuppressWarnings("unchecked")
    private void applyConfigToService(MockControllerClientBase service, Map<String, Object> config) {
        try {
            Class<?> clazz = service.getClass();
            Field[] fields = clazz.getDeclaredFields();
            
            // Применяем delays
            if (config.containsKey("delays")) {
                Map<String, Object> delays = (Map<String, Object>) config.get("delays");
                applyFields(service, fields, delays);
            }
            
            // Применяем intParams
            if (config.containsKey("intParams")) {
                Map<String, Object> intParams = (Map<String, Object>) config.get("intParams");
                applyFields(service, fields, intParams);
            }
            
            // Применяем booleanVariables
            if (config.containsKey("booleanVariables")) {
                Map<String, Object> booleanVariables = (Map<String, Object>) config.get("booleanVariables");
                applyFields(service, fields, booleanVariables);
            }
            
            // Применяем stringParams
            if (config.containsKey("stringParams")) {
                Map<String, Object> stringParams = (Map<String, Object>) config.get("stringParams");
                applyStringFields(service, fields, stringParams);
            }
            
        } catch (Exception e) {
            logger.error("Error applying config to {}: {}", service.getClass().getSimpleName(), e.getMessage(), e);
        }
    }
    
    /**
     * Применяет поля к сервису (для delays, intParams и booleanVariables).
     */
    private void applyFields(MockControllerClientBase service, Field[] fields, Map<String, Object> params) {
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String fieldName = entry.getKey();
            Field field = findField(fields, fieldName);
            
            if (field != null) {
                try {
                    field.setAccessible(true);
                    String stringValue = String.valueOf(entry.getValue());
                    Object value = parseValue(field.getType(), stringValue);
                    field.set(service, value);
                } catch (Exception e) {
                    logger.warn("Error setting field {} in {}: {}", fieldName, service.getClass().getSimpleName(), e.getMessage());
                }
            }
        }
    }
    
    /**
     * Применяет строковые поля к сервису (для stringParams).
     */
    private void applyStringFields(MockControllerClientBase service, Field[] fields, Map<String, Object> params) {
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String fieldName = entry.getKey();
            Field field = findField(fields, fieldName);
            
            if (field != null) {
                try {
                    field.setAccessible(true);
                    field.set(service, String.valueOf(entry.getValue()));
                } catch (Exception e) {
                    logger.warn("Error setting string field {} in {}: {}", fieldName, service.getClass().getSimpleName(), e.getMessage());
                }
            }
        }
    }
    
    /**
     * Находит поле по имени.
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
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (type == String.class) {
            return value;
        }
        return value;
    }
    
    /**
     * Формирует запрос для отправки в MockController.
     */
    private Map<String, Object> buildCheckUpdateRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put("SystemName", appConfig.getName());
        request.put("version", version);
        request.put("config", buildAggregatedConfig());
        return request;
    }
    
    /**
     * Проверяет обновления конфигурации в MockController.
     */
    public void checkUpdate() {
        try {
            if (mockControllerConfig == null || appConfig == null || loggingConfig == null) {
                logger.warn("MockController dependencies not initialized, skipping checkUpdate");
                return;
            }
            
            String url = mockControllerConfig.getUrl() + "/api/configs/checkUpdate";
            logger.info("Checking for config updates from MockController at: {}", url);
            
            Map<String, Object> requestBody = buildCheckUpdateRequest();
            logger.debug("Sending checkUpdate request with version: {}, systemName: {}", version, appConfig.getName());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<CheckUpdateResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                CheckUpdateResponse.class
            );
            
            CheckUpdateResponse responseBody = response.getBody();
            
            if (responseBody != null) {
                if (responseBody.getCurrentVersion() != null) {
                    this.version = responseBody.getCurrentVersion();
                }
                
                if (responseBody.isNeedUpdate()) {
                    logger.info("Config update required, loading version: {}", responseBody.getCurrentVersion());
                    loadAndApplyConfig(responseBody.getCurrentVersion());
                } else {
                    logger.debug("No config update needed. Current version: {}", responseBody.getCurrentVersion());
                }
            } else {
                logger.warn("Empty response received from MockController");
            }
            
        } catch (Exception e) {
            logger.error("Error calling checkUpdate in MockController: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Загружает конфигурацию из MockController и применяет её ко всем сервисам.
     */
    private void loadAndApplyConfig(String version) {
        try {
            if (mockControllerConfig == null || appConfig == null) {
                logger.warn("MockController dependencies not initialized, cannot load config");
                return;
            }
            
            String systemName = appConfig.getName();
            String url = mockControllerConfig.getUrl() + "/api/configs/" + systemName + "?version=" + version;
            
            ResponseEntity<ConfigResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                ConfigResponse.class
            );
            
            ConfigResponse configResponse = response.getBody();
            
            if (configResponse != null && configResponse.getConfig() != null) {
                applyConfigToAllServices(configResponse.getConfig());
                
                // Обновляем уровень логирования
                if (configResponse.getConfig().containsKey("loggingLv")) {
                    String loggingLv = String.valueOf(configResponse.getConfig().get("loggingLv"));
                    applyLoggingLevel(loggingLv);
                    loggingConfig.setLoggingLevel(loggingLv);
                }
                
                this.version = configResponse.getVersion();
                logger.info("Config applied successfully to all services, version: {}", configResponse.getVersion());
            } else {
                logger.warn("Empty response received when loading config");
            }
            
        } catch (Exception e) {
            logger.error("Error loading config from MockController: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Применяет уровень логирования.
     */
    private void applyLoggingLevel(String levelStr) {
        try {
            ch.qos.logback.classic.LoggerContext loggerContext = 
                (ch.qos.logback.classic.LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
            ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger("com.mock");
            
            ch.qos.logback.classic.Level level;
            switch (levelStr.toUpperCase()) {
                case "ERROR":
                    level = ch.qos.logback.classic.Level.ERROR;
                    break;
                case "WARN":
                    level = ch.qos.logback.classic.Level.WARN;
                    break;
                case "INFO":
                    level = ch.qos.logback.classic.Level.INFO;
                    break;
                case "DEBUG":
                    level = ch.qos.logback.classic.Level.DEBUG;
                    break;
                case "TRACE":
                    level = ch.qos.logback.classic.Level.TRACE;
                    break;
                default:
                    logger.warn("Unknown logging level: {}, using INFO", levelStr);
                    level = ch.qos.logback.classic.Level.INFO;
                    break;
            }
            
            rootLogger.setLevel(level);
        } catch (Exception e) {
            logger.error("Error setting logging level: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Вызывается при готовности приложения.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        checkUpdate();
    }
    
    /**
     * Периодически проверяет обновления конфигурации.
     */
    @Scheduled(fixedDelayString = "${mock-controller.check-interval-seconds:5}000")
    public void scheduledCheckUpdate() {
        lastCheckUpdateTime = System.currentTimeMillis();
        checkUpdateCount++;
        logger.info("Scheduled checkUpdate #{} triggered at {}", checkUpdateCount, new java.util.Date(lastCheckUpdateTime));
        checkUpdate();
    }
    
    /**
     * Возвращает информацию о последней проверке конфигурации.
     */
    public Map<String, Object> getCheckUpdateStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("lastCheckUpdateTime", lastCheckUpdateTime > 0 ? new java.util.Date(lastCheckUpdateTime).toString() : "Never");
        status.put("checkUpdateCount", checkUpdateCount);
        status.put("currentVersion", version);
        status.put("checkIntervalSeconds", mockControllerConfig != null ? mockControllerConfig.getCheckIntervalSeconds() : 5);
        status.put("mockControllerUrl", mockControllerConfig != null ? mockControllerConfig.getUrl() : "Not configured");
        long timeSinceLastCheck = lastCheckUpdateTime > 0 ? (System.currentTimeMillis() - lastCheckUpdateTime) / 1000 : -1;
        status.put("secondsSinceLastCheck", timeSinceLastCheck);
        status.put("servicesCount", getAllConfigurableServices().size());
        return status;
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

