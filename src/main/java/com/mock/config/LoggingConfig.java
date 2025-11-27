package com.mock.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingConfig {
    
    @Value("${logging.logback.level:INFO}")
    private String loggingLevel = "INFO";
    
    public String getLoggingLevel() {
        return loggingLevel;
    }
    
    public void setLoggingLevel(String loggingLevel) {
        this.loggingLevel = loggingLevel;
    }
    
    /**
     * @deprecated Используйте getLoggingLevel()
     */
    @Deprecated
    public String getComMock() {
        return loggingLevel;
    }
    
    /**
     * @deprecated Используйте setLoggingLevel()
     */
    @Deprecated
    public void setComMock(String comMock) {
        this.loggingLevel = comMock;
    }
}

