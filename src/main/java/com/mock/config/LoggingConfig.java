package com.mock.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация для управления уровнем логирования.
 * Читает начальный уровень из application.yml (logging.level.root или logging.logback.level).
 * Уровень может быть динамически изменен через MockController.
 */
@Configuration
public class LoggingConfig {
    
    /**
     * Читает уровень логирования из application.yml.
     * Приоритет: logging.level.root > logging.logback.level > INFO (по умолчанию)
     */
    @Value("${logging.level.root:${logging.logback.level:INFO}}")
    private String loggingLevel = "INFO";
    
    public String getLoggingLevel() {
        return loggingLevel;
    }
    
    public void setLoggingLevel(String loggingLevel) {
        this.loggingLevel = loggingLevel;
    }
}

