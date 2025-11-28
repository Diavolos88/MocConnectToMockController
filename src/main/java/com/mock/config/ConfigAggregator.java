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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    
    private volatile RestTemplate restTemplate;
    private volatile RestTemplate healthcheckRestTemplate;
    private volatile String version = "v1";
    private volatile long lastCheckUpdateTime = 0;
    private volatile int checkUpdateCount = 0;
    private volatile int errorCount = 0;
    private volatile String lastError = null;
    private volatile boolean isMockControllerHealthy = true; // По умолчанию считаем здоровым
    private volatile long lastHealthcheckTime = 0;
    private volatile int healthcheckCount = 0;
    private volatile int healthcheckFailureCount = 0;
    
    /**
     * Инициализация RestTemplate после инъекции зависимостей.
     */
    @Autowired
    public synchronized void initializeRestTemplates() {
        if (mockControllerConfig != null) {
            this.restTemplate = createRestTemplateWithTimeouts(
                mockControllerConfig.getConnectTimeoutSeconds(), 
                mockControllerConfig.getReadTimeoutSeconds()
            );
            this.healthcheckRestTemplate = createRestTemplateWithTimeouts(
                mockControllerConfig.getHealthcheckTimeoutSeconds(),
                mockControllerConfig.getHealthcheckTimeoutSeconds()
            );
        } else {
            // Fallback значения если конфиг еще не загружен
            this.restTemplate = createRestTemplateWithTimeouts(10, 10);
            this.healthcheckRestTemplate = createRestTemplateWithTimeouts(5, 5);
        }
    }
    
    /**
     * Создает RestTemplate с настроенными таймаутами для предотвращения блокировки при недоступности MockController.
     */
    private RestTemplate createRestTemplateWithTimeouts(long connectTimeoutSeconds, long readTimeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(connectTimeoutSeconds));
        factory.setReadTimeout((int) TimeUnit.SECONDS.toMillis(readTimeoutSeconds));
        return new RestTemplate(factory);
    }
    
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
            if (prefix == null || prefix.isEmpty()) {
                logger.warn("Prefix is null or empty, skipping field extraction");
                return result;
            }
            
            String prefixLower = prefix.toLowerCase();
            String prefixUpper = prefix.toUpperCase();
            String prefixCapitalized = prefix.length() > 0 
                ? prefix.substring(0, 1).toUpperCase() + prefix.substring(1).toLowerCase()
                : prefix;
            
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
                Object delaysObj = config.get("delays");
                if (delaysObj instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> delays = (Map<String, Object>) delaysObj;
                    applyFields(service, fields, delays);
                }
            }
            
            // Применяем intParams
            if (config.containsKey("intParams")) {
                Object intParamsObj = config.get("intParams");
                if (intParamsObj instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> intParams = (Map<String, Object>) intParamsObj;
                    applyFields(service, fields, intParams);
                }
            }
            
            // Применяем booleanVariables
            if (config.containsKey("booleanVariables")) {
                Object booleanVariablesObj = config.get("booleanVariables");
                if (booleanVariablesObj instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> booleanVariables = (Map<String, Object>) booleanVariablesObj;
                    applyFields(service, fields, booleanVariables);
                }
            }
            
            // Применяем stringParams
            if (config.containsKey("stringParams")) {
                Object stringParamsObj = config.get("stringParams");
                if (stringParamsObj instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> stringParams = (Map<String, Object>) stringParamsObj;
                    applyStringFields(service, fields, stringParams);
                }
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
                } catch (NumberFormatException e) {
                    logger.warn("Failed to parse value for field {} in {}: {}. Value: {}", 
                        fieldName, service.getClass().getSimpleName(), e.getMessage(), entry.getValue());
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid value for field {} in {}: {}", 
                        fieldName, service.getClass().getSimpleName(), e.getMessage());
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
     * @throws NumberFormatException если значение не может быть распарсено
     */
    private Object parseValue(Class<?> type, String value) throws NumberFormatException {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        
        try {
            if (type == long.class || type == Long.class) {
                return Long.parseLong(value);
            } else if (type == int.class || type == Integer.class) {
                return Integer.parseInt(value);
            } else if (type == boolean.class || type == Boolean.class) {
                return Boolean.parseBoolean(value);
            } else if (type == String.class) {
                return value;
            }
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse value '{}' as type {}: {}", value, type.getSimpleName(), e.getMessage());
            throw e;
        }
        
        return value;
    }
    
    /**
     * Формирует запрос для отправки в MockController.
     */
    private Map<String, Object> buildCheckUpdateRequest() {
        Map<String, Object> request = new HashMap<>();
        if (appConfig != null && appConfig.getName() != null) {
            request.put("SystemName", appConfig.getName());
        }
        request.put("version", version);
        request.put("config", buildAggregatedConfig());
        return request;
    }
    
    /**
     * Проверяет обновления конфигурации в MockController.
     */
    public void checkUpdate() {
        // Не выполняем checkUpdate если MockController не здоров
        if (!isMockControllerHealthy) {
            logger.debug("Skipping checkUpdate: MockController is not healthy");
            return;
        }
        
        try {
            if (mockControllerConfig == null || appConfig == null || loggingConfig == null) {
                logger.warn("MockController dependencies not initialized, skipping checkUpdate");
                return;
            }
            
            // Инициализируем RestTemplate если еще не инициализирован
            if (restTemplate == null) {
                initializeRestTemplates();
            }
            
            if (restTemplate == null) {
                logger.error("Failed to initialize RestTemplate, skipping checkUpdate");
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
                    String currentVersion = responseBody.getCurrentVersion();
                    if (currentVersion != null) {
                        logger.info("Config update required, loading version: {}", currentVersion);
                        loadAndApplyConfig(currentVersion);
                    } else {
                        logger.warn("Config update required but currentVersion is null, skipping");
                    }
                } else {
                    logger.debug("No config update needed. Current version: {}", responseBody.getCurrentVersion());
                }
            } else {
                logger.warn("Empty response received from MockController");
            }
            
        } catch (RestClientException e) {
            errorCount++;
            lastError = e.getMessage();
            logger.warn("MockController unavailable or error occurred (error #{}): {}. Application continues to work normally.", 
                errorCount, e.getMessage());
        } catch (Exception e) {
            errorCount++;
            lastError = e.getMessage();
            logger.error("Unexpected error calling checkUpdate in MockController (error #{}): {}", 
                errorCount, e.getMessage(), e);
        }
    }
    
    /**
     * Загружает конфигурацию из MockController и применяет её ко всем сервисам.
     */
    private void loadAndApplyConfig(String version) {
        try {
            if (version == null || version.isEmpty()) {
                logger.warn("Version is null or empty, cannot load config");
                return;
            }
            
            if (mockControllerConfig == null || appConfig == null) {
                logger.warn("MockController dependencies not initialized, cannot load config");
                return;
            }
            
            // Инициализируем RestTemplate если еще не инициализирован
            if (restTemplate == null) {
                initializeRestTemplates();
            }
            
            if (restTemplate == null) {
                logger.error("Failed to initialize RestTemplate, cannot load config");
                return;
            }
            
            String systemName = appConfig.getName();
            if (systemName == null || systemName.isEmpty()) {
                logger.warn("System name is null or empty, cannot load config");
                return;
            }
            
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
            
        } catch (RestClientException e) {
            logger.warn("MockController unavailable when loading config: {}. Application continues to work with current configuration.", 
                e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error loading config from MockController: {}", e.getMessage(), e);
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
        performHealthCheck();
        if (isMockControllerHealthy) {
            checkUpdate();
        }
    }
    
    /**
     * Периодически проверяет healthcheck MockController.
     */
    @Scheduled(fixedDelayString = "${mock-controller.healthcheck-interval-seconds:10}000")
    public void scheduledHealthCheck() {
        lastHealthcheckTime = System.currentTimeMillis();
        healthcheckCount++;
        logger.info("Scheduled healthcheck #{} triggered at {}", healthcheckCount, new java.util.Date(lastHealthcheckTime));
        performHealthCheck();
    }
    
    /**
     * Периодически проверяет обновления конфигурации.
     */
    @Scheduled(fixedDelayString = "${mock-controller.check-interval-seconds:5}000")
    public void scheduledCheckUpdate() {
        // Не выполняем checkUpdate если MockController не здоров
        if (!isMockControllerHealthy) {
            logger.debug("Skipping scheduled checkUpdate: MockController is not healthy");
            return;
        }
        
        lastCheckUpdateTime = System.currentTimeMillis();
        checkUpdateCount++;
        logger.info("Scheduled checkUpdate #{} triggered at {}", checkUpdateCount, new java.util.Date(lastCheckUpdateTime));
        checkUpdate();
    }
    
    /**
     * Выполняет healthcheck MockController.
     * Если ответ не 200 или занимает больше таймаута, считаем MockController нездоровым.
     */
    public void performHealthCheck() {
        try {
            if (mockControllerConfig == null) {
                logger.warn("MockController config not initialized, skipping healthcheck");
                isMockControllerHealthy = false;
                return;
            }
            
            if (healthcheckRestTemplate == null) {
                initializeRestTemplates();
            }
            
            if (healthcheckRestTemplate == null) {
                logger.error("Failed to initialize healthcheckRestTemplate, marking as unhealthy");
                isMockControllerHealthy = false;
                return;
            }
            
            String healthcheckUrl = mockControllerConfig.getUrl() + mockControllerConfig.getHealthcheckPath();
            logger.debug("Performing healthcheck at: {}", healthcheckUrl);
            
            long startTime = System.currentTimeMillis();
            ResponseEntity<String> response = healthcheckRestTemplate.exchange(
                healthcheckUrl,
                HttpMethod.GET,
                null,
                String.class
            );
            long duration = System.currentTimeMillis() - startTime;
            
            // Проверяем статус 200
            int statusCode = response.getStatusCode().value();
            if (statusCode == 200) {
                isMockControllerHealthy = true;
                healthcheckFailureCount = 0;
                logger.debug("Healthcheck successful: status=200, duration={}ms", duration);
            } else {
                isMockControllerHealthy = false;
                healthcheckFailureCount++;
                logger.warn("Healthcheck failed: status={}, duration={}ms (failure #{})", 
                    statusCode, duration, healthcheckFailureCount);
            }
            
        } catch (RestClientException e) {
            isMockControllerHealthy = false;
            healthcheckFailureCount++;
            logger.warn("Healthcheck failed: {} (failure #{})", e.getMessage(), healthcheckFailureCount);
        } catch (Exception e) {
            isMockControllerHealthy = false;
            healthcheckFailureCount++;
            logger.error("Unexpected error during healthcheck (failure #{}): {}", 
                healthcheckFailureCount, e.getMessage(), e);
        }
    }
    
    /**
     * Возвращает информацию о последней проверке конфигурации.
     */
    public Map<String, Object> getCheckUpdateStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("lastCheckUpdateTime", lastCheckUpdateTime > 0 ? new java.util.Date(lastCheckUpdateTime).toString() : "Never");
        status.put("checkUpdateCount", checkUpdateCount);
        status.put("errorCount", errorCount);
        status.put("lastError", lastError != null ? lastError : "None");
        status.put("currentVersion", version);
        status.put("checkIntervalSeconds", mockControllerConfig != null ? mockControllerConfig.getCheckIntervalSeconds() : 5);
        status.put("mockControllerUrl", mockControllerConfig != null ? mockControllerConfig.getUrl() : "Not configured");
        long timeSinceLastCheck = lastCheckUpdateTime > 0 ? (System.currentTimeMillis() - lastCheckUpdateTime) / 1000 : -1;
        status.put("secondsSinceLastCheck", timeSinceLastCheck);
        status.put("servicesCount", getAllConfigurableServices().size());
        status.put("isMockControllerAvailable", errorCount == 0 || (timeSinceLastCheck >= 0 && timeSinceLastCheck < 10));
        
        // Healthcheck информация
        status.put("isMockControllerHealthy", isMockControllerHealthy);
        status.put("lastHealthcheckTime", lastHealthcheckTime > 0 ? new java.util.Date(lastHealthcheckTime).toString() : "Never");
        status.put("healthcheckCount", healthcheckCount);
        status.put("healthcheckFailureCount", healthcheckFailureCount);
        status.put("healthcheckPath", mockControllerConfig != null ? mockControllerConfig.getHealthcheckPath() : "/service/healthcheck");
        status.put("healthcheckIntervalSeconds", mockControllerConfig != null ? mockControllerConfig.getHealthcheckIntervalSeconds() : 10);
        long timeSinceLastHealthcheck = lastHealthcheckTime > 0 ? (System.currentTimeMillis() - lastHealthcheckTime) / 1000 : -1;
        status.put("secondsSinceLastHealthcheck", timeSinceLastHealthcheck);
        
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

